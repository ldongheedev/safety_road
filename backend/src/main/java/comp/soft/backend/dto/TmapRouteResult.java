package comp.soft.backend.dto;

import java.util.List;

public class TmapRouteResult {

    private List<double[]> coordinates;
    private int totalDistance;
    private int totalTime;
    private String searchOption;

    public TmapRouteResult(List<double[]> coordinates, int totalDistance, int totalTime, String searchOption) {
        this.coordinates = coordinates;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.searchOption = searchOption;
    }

    public List<double[]> getCoordinates() { return coordinates; }
    public int getTotalDistance() { return totalDistance; }
    public int getTotalTime() { return totalTime; }
    public String getSearchOption() { return searchOption; }
}
