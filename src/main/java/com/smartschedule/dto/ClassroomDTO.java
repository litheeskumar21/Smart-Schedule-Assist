package com.smartschedule.dto;

import com.smartschedule.model.enums.RoomType;
import lombok.Data;

@Data
public class ClassroomDTO {
    private String name;
    private RoomType roomType;
    private int capacity;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoomType getRoomType() {
        return this.roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
