package com.gaia3d.modifier.billboard;

import com.gaia3d.modifier.billboard.merge.MergeConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class BillboardCloudOptions {
    @Builder.Default
    private final double normalDotThreshold = Math.cos(Math.toRadians(30)); // 법선 유사도 기준
    @Builder.Default
    private final double planeDistanceEpsilon = 0.1; // 후보 평면이 시드 평면에서 얼마나 떨어져 있어야 하는지
    @Builder.Default
    private final double setRadius = 0.3; // 시드 반경
    @Builder.Default
    private final double minTriangleArea = 1e-6; // 삼각형 최소 면적
    @Builder.Default
    private final double maxPlaneExtent = 0.35; // 평면 최대 확장
    @Builder.Default
    private final int maxSplitDepth = 2; // 최대 분할 깊이 (2면 최대 4등분 재귀 2단계)
    @Builder.Default
    private final int minClusterFaceCount = 1; // 클러스터가 유지되기 위한 최소 면 수
    @Builder.Default
    private final double splitBalanceEpsilon = 0.05; // 분할 균형 허용 오차 (0.05면 5% 이상 불균형한 경우 분할)
    @Builder.Default
    private final int maxBillboardPlaneCount = 0;
    @Builder.Default
    private final int planeBudgetMergePasses = 6;
    @Builder.Default
    private final double quadOverlapRatio = 0.0;
    @Builder.Default
    private final MergeConfig mergeConfig = new MergeConfig();

    @Builder.Default
    private final boolean refineBillboardPlane = true;
    @Builder.Default
    private final int maximumTextureSize = 128;
    @Builder.Default
    private final boolean blendTexture = false;
    @Builder.Default
    private String tempPath = "/temp";
}
