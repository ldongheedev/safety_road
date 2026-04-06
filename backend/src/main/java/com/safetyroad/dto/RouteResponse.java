package com.safetyroad.dto;

import java.util.List;

public class RouteResponse {

    private String routeId;
    private List<double[]> coordinates;
    private int totalDistance;
    private int totalTime;
    private Double safetyScore;
    private boolean isRecommended;
    private String searchOption;

    public RouteResponse(String routeId, List<double[]> coordinates, int totalDistance,
                         int totalTime, Double safetyScore, boolean isRecommended, String searchOption) {
        this.routeId = routeId;
        this.coordinates = coordinates;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.safetyScore = safetyScore;
        this.isRecommended = isRecommended;
        this.searchOption = searchOption;
    }

    public String getRouteId() { return routeId; }
    public List<double[]> getCoordinates() { return coordinates; }
    public int getTotalDistance() { return totalDistance; }
    public int getTotalTime() { return totalTime; }
    public Double getSafetyScore() { return safetyScore; }
    public boolean isRecommended() { return isRecommended; }
    public String getSearchOption() { return searchOption; }
}
