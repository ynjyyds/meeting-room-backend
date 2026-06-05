package com.meeting;

import javax.persistence.*;

@Entity
@Table(name = "meeting_room")
public class MeetingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer capacity;

    private String location;

    @Column(name = "image_url")
    private String imageUrl;

    private String facilities;

    private Integer status = 1; // 1:可用 0:停用

    // 无参构造
    public MeetingRoom() {
    }

    // 有参构造
    public MeetingRoom(String name, Integer capacity, String location, String imageUrl, String facilities) {
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.imageUrl = imageUrl;
        this.facilities = facilities;
        this.status = 1;
    }

    // Getter 和 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}