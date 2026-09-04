package com.feedstartup.dto;

/** Overview numbers shown on the EPM page's stats card - computed from real rows instead of
 * being hardcoded copy. */
public class EpmStatsDto {

    private final long epmsConducted;
    private final long districtsCovered;
    private final long totalParticipants;
    private final long upcomingCount;

    public EpmStatsDto(long epmsConducted, long districtsCovered, long totalParticipants, long upcomingCount) {
        this.epmsConducted = epmsConducted;
        this.districtsCovered = districtsCovered;
        this.totalParticipants = totalParticipants;
        this.upcomingCount = upcomingCount;
    }

    public long getEpmsConducted() { return epmsConducted; }
    public long getDistrictsCovered() { return districtsCovered; }
    public long getTotalParticipants() { return totalParticipants; }
    public long getUpcomingCount() { return upcomingCount; }
}
