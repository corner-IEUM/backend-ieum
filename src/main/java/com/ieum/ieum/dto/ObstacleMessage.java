package com.ieum.ieum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObstacleMessage {

    private String type;
    private Integer trackId;
    private String className;
    private String direction;
    private Double proximity;
    private String message;
    private Double timestamp;
}