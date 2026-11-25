package com.storm.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class RotationHandler {

    // the yaw (degrees) you'd need to face to look from (fromX, fromZ) toward (toX, toZ)
    public static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    // the shortest signed turn from current to target, in the range -180..180
    public static float angleDiff(float current, float target) {
        return Mth.wrapDegrees(target - current);
    }

    // one step from current toward target, turning at most maxStep degrees
    public static float stepToward(float current, float target, float maxStep) {
        float diff = angleDiff(current, target);
        float step = Mth.clamp(diff, -maxStep, maxStep);
        return current + step;
    }

    // turn the player a little toward a point; call this once per tick
    public static void faceToward(LocalPlayer player, double targetX, double targetZ, float maxStep) {
        float wanted = yawTo(player.getX(), player.getZ(), targetX, targetZ);
        float newYaw = stepToward(player.getYRot(), wanted, maxStep);
        player.setYRot(newYaw);
    }

    // the gentle pitch (degrees) for looking toward a point dy higher/lower, horizDist away
    public static float pitchTo(double dy, double horizDist, float strength, float maxPitch) {
        double raw = -Math.toDegrees(Math.atan2(dy, Math.max(horizDist, 0.001)));
        return Mth.clamp((float) raw * strength, -maxPitch, maxPitch);
    }

    // one step from the current pitch toward the wanted one (no wraparound needed for pitch)
    public static float stepPitchToward(float current, float target, float maxStep) {
        float step = Mth.clamp(target - current, -maxStep, maxStep);
        return current + step;
    }

}