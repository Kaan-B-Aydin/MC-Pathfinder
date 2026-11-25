package com.storm.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.Random;

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


    // ================= smooth, human-like aiming =================
    // The follower sets a target once per tick; updateFrame() moves the camera toward it every
    // rendered frame using a spring (fast start, eases in, tiny overshoot on big turns) plus a
    // little slow drift, so the view never looks locked or perfectly linear.

    private static final float YAW_STIFFNESS = 55f;        // higher = snappier turns
    private static final float PITCH_STIFFNESS = 40f;
    private static final float YAW_DAMPING_RATIO = 0.82f;  // < 1 gives a slight, natural overshoot
    private static final float PITCH_DAMPING_RATIO = 1.0f;
    private static final float MAX_YAW_SPEED = 420f;       // degrees per second
    private static final float MAX_PITCH_SPEED = 180f;
    private static final float DRIFT_YAW = 0.6f;           // degrees of idle wander
    private static final float DRIFT_PITCH = 0.4f;

    private static final Random RNG = new Random();

    private static boolean aiming = false;
    private static float targetYaw, targetPitch;
    private static float yawVel, pitchVel;
    private static float speedMul = 1f;                    // per-turn speed variation
    private static float driftYaw, driftPitch, driftYawGoal, driftPitchGoal;
    private static long lastFrameNanos = 0;

    // set where the camera should end up; call each tick while a path is active
    public static void setTarget(float yaw, float pitch) {
        if (!aiming) {
            yawVel = 0;
            pitchVel = 0;
        }
        // a new, clearly different heading: pick a fresh speed so turns don't all feel identical
        if (!aiming || Math.abs(angleDiff(targetYaw, yaw)) > 25f) {
            speedMul = 0.8f + RNG.nextFloat() * 0.45f;
        }
        aiming = true;
        targetYaw = yaw;
        targetPitch = pitch;

        // slowly wandering offset; a new goal now and then, followed lazily
        if (RNG.nextInt(25) == 0) {
            driftYawGoal = (RNG.nextFloat() * 2 - 1) * DRIFT_YAW;
            driftPitchGoal = (RNG.nextFloat() * 2 - 1) * DRIFT_PITCH;
        }
        driftYaw += (driftYawGoal - driftYaw) * 0.08f;
        driftPitch += (driftPitchGoal - driftPitch) * 0.08f;
    }

    public static void clearTarget() {
        aiming = false;
        yawVel = 0;
        pitchVel = 0;
    }

    // the target yaw the follower last asked for
    public static float getTargetYaw() {
        return targetYaw;
    }

    // call once per rendered frame
    public static void updateFrame(LocalPlayer player) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        if (!aiming || player == null || dt <= 0f) return;
        dt = Math.min(dt, 0.05f);                          // keep the spring stable after a hitch

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        float kYaw = YAW_STIFFNESS * speedMul;
        float yawDiff = angleDiff(yaw, targetYaw + driftYaw);
        yawVel += (kYaw * yawDiff - 2f * YAW_DAMPING_RATIO * (float) Math.sqrt(kYaw) * yawVel) * dt;
        yawVel = Mth.clamp(yawVel, -MAX_YAW_SPEED * speedMul, MAX_YAW_SPEED * speedMul);

        float kPitch = PITCH_STIFFNESS * speedMul;
        float pitchDiff = (targetPitch + driftPitch) - pitch;
        pitchVel += (kPitch * pitchDiff - 2f * PITCH_DAMPING_RATIO * (float) Math.sqrt(kPitch) * pitchVel) * dt;
        pitchVel = Mth.clamp(pitchVel, -MAX_PITCH_SPEED, MAX_PITCH_SPEED);

        player.setYRot(yaw + yawVel * dt);
        player.setXRot(Mth.clamp(pitch + pitchVel * dt, -90f, 90f));
    }
}