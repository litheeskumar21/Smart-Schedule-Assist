"""
Google OR-Tools CP-SAT Timetable Optimization Engine
====================================================
Integrates with Smart Schedule Assist to solve constraint-based timetables
using integer programming and SAT solvers.
"""

import sys
import json
import argparse
from typing import Dict, Any, List

def solve_timetable(problem_data: Dict[str, Any]) -> Dict[str, Any]:
    try:
        from ortools.sat.python import cp_model
    except ImportError:
        return {
            "status": "ERROR",
            "solver": "Google OR-Tools CP-SAT",
            "message": "Google OR-Tools is not installed in the local Python environment. Please run: pip install ortools",
            "entries": [],
            "conflicts": ["ortools package missing"]
        }

    model = cp_model.CpModel()

    section_id = problem_data.get("sectionId", 1)
    subjects = problem_data.get("subjects", [])
    classrooms = problem_data.get("classrooms", [])
    periods = problem_data.get("periods", [])
    working_days = problem_data.get("workingDays", ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"])
    locked_entries = problem_data.get("lockedEntries", [])
    unavailabilities = problem_data.get("unavailabilities", [])
    max_daily_periods = problem_data.get("maxDailyTeacherPeriods", 4)
    max_weekly_periods = problem_data.get("maxWeeklyTeacherPeriods", 18)

    # Decision variables: x[sub_id, day, period_num, room_id] = 1 if assigned
    x = {}
    for sub in subjects:
        sub_id = sub["id"]
        for day in working_days:
            for p in periods:
                p_num = p["periodNumber"]
                for r in classrooms:
                    r_id = r["id"]
                    x[(sub_id, day, p_num, r_id)] = model.NewBoolVar(f"x_{sub_id}_{day}_{p_num}_{r_id}")

    # Constraint 1: At most one subject per time slot in this section
    for day in working_days:
        for p in periods:
            p_num = p["periodNumber"]
            slot_vars = [x[(sub["id"], day, p_num, r["id"])] for sub in subjects for r in classrooms]
            model.Add(sum(slot_vars) <= 1)

    # Constraint 2: Subject demand fulfillment
    for sub in subjects:
        sub_id = sub["id"]
        required = sub.get("periodsPerWeek", 3)
        sub_vars = [x[(sub_id, day, p["periodNumber"], r["id"])] for day in working_days for p in periods for r in classrooms]
        model.Add(sum(sub_vars) == required)

    # Constraint 3: Room capability (Labs must be in LAB rooms)
    for sub in subjects:
        sub_id = sub["id"]
        is_lab = sub.get("isLab", False)
        for r in classrooms:
            r_id = r["id"]
            is_lab_room = r.get("roomType") in ["COMPUTER_LAB", "PHYSICS_LAB", "ELECTRONICS_LAB", "WORKSHOP"]
            if is_lab and not is_lab_room:
                for day in working_days:
                    for p in periods:
                        model.Add(x[(sub_id, day, p["periodNumber"], r_id)] == 0)

    # Constraint 4: Teacher availability matrix
    for unavail in unavailabilities:
        t_id = unavail.get("teacherId")
        u_day = unavail.get("dayOfWeek")
        u_period = unavail.get("periodNumber")
        for sub in subjects:
            if sub.get("teacherId") == t_id:
                sub_id = sub["id"]
                for r in classrooms:
                    if (sub_id, u_day, u_period, r["id"]) in x:
                        model.Add(x[(sub_id, u_day, u_period, r["id"])] == 0)

    # Constraint 5: Locked slots preservation
    for locked in locked_entries:
        l_sub = locked.get("subjectId")
        l_day = locked.get("dayOfWeek")
        l_period = locked.get("periodNumber")
        l_room = locked.get("classroomId")
        if (l_sub, l_day, l_period, l_room) in x:
            model.Add(x[(l_sub, l_day, l_period, l_room)] == 1)

    # Objective: Soft constraints (maximize morning theory, spread across days)
    objective_terms = []
    for sub in subjects:
        sub_id = sub["id"]
        for day in working_days:
            for p in periods:
                p_num = p["periodNumber"]
                for r in classrooms:
                    var = x[(sub_id, day, p_num, r["id"])]
                    # Morning preference (+3 score)
                    if p_num <= 4:
                        objective_terms.append(var * 3)

    model.Maximize(sum(objective_terms))

    # Solve
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = 10.0
    status = solver.Solve(model)

    if status in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        assigned_entries = []
        for (sub_id, day, p_num, r_id), var in x.items():
            if solver.Value(var) == 1:
                assigned_entries.append({
                    "subjectId": sub_id,
                    "dayOfWeek": day,
                    "periodNumber": p_num,
                    "classroomId": r_id
                })

        return {
            "status": "OPTIMAL" if status == cp_model.OPTIMAL else "FEASIBLE",
            "solver": "Google OR-Tools CP-SAT",
            "valid": True,
            "classesScheduled": len(assigned_entries),
            "entries": assigned_entries,
            "wallTimeSeconds": solver.WallTime(),
            "conflicts": []
        }
    else:
        return {
            "status": "INFEASIBLE",
            "solver": "Google OR-Tools CP-SAT",
            "valid": False,
            "classesScheduled": 0,
            "entries": [],
            "conflicts": [
                "Constraints cannot be satisfied simultaneously with current teacher/classroom limits.",
                "Verify required subject periods per week against available free slots."
            ]
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="Input JSON path")
    parser.add_argument("--output", required=True, help="Output JSON path")
    args = parser.parse_args()

    with open(args.input, "r") as f:
        data = json.load(f)

    result = solve_timetable(data)

    with open(args.output, "w") as f:
        json.dump(result, f, indent=2)

    print(f"Solvler finished with status: {result['status']}")
