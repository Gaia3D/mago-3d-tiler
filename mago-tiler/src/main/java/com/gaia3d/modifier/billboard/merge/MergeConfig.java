package com.gaia3d.modifier.billboard.merge;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MergeConfig {
    public double minNormalDot = Math.cos(Math.toRadians(20)); // 법선 유사도 기준
    public double maxPlaneDistance = 0.3; // 평면과 병합 후보 사이의 최대 거리
    public double minEfficiency = 0.1; // 병합 효율성 기준 (병합으로 인해 줄어드는 면적 비율, 0.1이면 10% 이상 줄어들어야 병합)
    public double maxThickness = 0.1; // 최대 두께 (평면과 병합 후보 사이의 최대 거리)
    public double maxCenterDistance = 0.2; // (병합 후보의 중심과 시드 평면의 중심 사이의 최대 거리
    public double maxCenterDistanceSquared = maxCenterDistance * maxCenterDistance; // maxCenterDistance의 제곱 (거리 비교 시 제곱근 계산을 피하기 위해)
    public double maxRectGap = 0.05; // 최대 사각형 간격 (병합 후보의 경계 사각형과 시드 평면의 경계 사각형 사이의 최대 간격)

    /**
     * grid cell size.
     * 기본값은 maxCenterDistance 와 동일하게 맞춰두는 게 안전함.
     */
    public double gridCellSize = maxCenterDistance;

    public MergeConfig copy() {
        MergeConfig copy = new MergeConfig();
        copy.minNormalDot = minNormalDot;
        copy.maxPlaneDistance = maxPlaneDistance;
        copy.minEfficiency = minEfficiency;
        copy.maxThickness = maxThickness;
        copy.maxCenterDistance = maxCenterDistance;
        copy.maxCenterDistanceSquared = maxCenterDistanceSquared;
        copy.maxRectGap = maxRectGap;
        copy.gridCellSize = gridCellSize;
        return copy;
    }

    public void prepare() {
        maxCenterDistanceSquared = maxCenterDistance * maxCenterDistance;
        if (gridCellSize <= 1e-12) {
            gridCellSize = maxCenterDistance;
        }
        if (gridCellSize <= 1e-12) {
            gridCellSize = 1.0;
        }
    }
}
