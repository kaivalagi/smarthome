/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.dmx.internal.action;

import org.eclipse.smarthome.binding.dmx.internal.Util;
import org.eclipse.smarthome.binding.dmx.internal.multiverse.Channel;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * The {@link FadeAction} fades a given channel from its current state to the requested
 * state in the given amount of time. After the fade, the new state is held for
 * a given or indefinite time.
 *
 * @author Davy Vanherbergen
 * @author Jan N. Klug
 */
public class FadeAction extends BaseAction {
    /** Time in ms to hold the target value. -1 is indefinite */
    private long holdTime;

    /** Time in ms to fade from current value to new target value */
    private long fadeTime;

    /** Channel output value on action start. **/
    private int startValue;

    /** Desired channel output value. **/
    private int targetValue;

    private float stepDuration;

    private FadeDirection fadeDirection;

    /**
     * Create new fading action.
     *
     * @param fadeTime time in ms to fade from the current value to the new value.
     * @param targetValue new value 0-255 for this channel.
     * @param holdTime time in ms to hold the color before moving to the next action. -1 is indefinite.
     */
    public FadeAction(int fadeTime, int targetValue, int holdTime) {
        super();

        this.fadeTime = fadeTime;
        this.targetValue = Util.toDmxValue(targetValue) << 8;
        this.holdTime = holdTime;

        if (holdTime < -1) {
            this.holdTime = -1;
        }
        if (fadeTime < 0) {
            this.fadeTime = 0;
        }
    }

    public FadeAction(int fadeTime, PercentType targetValue, int holdTime) {
        this(fadeTime, Util.toDmxValue(targetValue), holdTime);
    }

    public FadeAction(int fadeTime, int currentValue, int targetValue, int holdTime) {
        this(Util.fadeTimeFraction(currentValue, targetValue, fadeTime), targetValue, holdTime);
    }

    @Override
    public int getNewValue(Channel channel, long currentTime) {
        int newValue = channel.getHiResValue();

        if (startTime == 0) {
            startTime = currentTime;

            if (fadeTime != 0) {
                startValue = channel.getHiResValue();

                // calculate fade details
                if (startValue == targetValue) {
                    stepDuration = 1;
                } else if (startValue > targetValue) {
                    fadeDirection = FadeDirection.down;
                    stepDuration = (float) fadeTime / (startValue - targetValue);
                } else {
                    fadeDirection = FadeDirection.up;
                    stepDuration = (float) fadeTime / (targetValue - startValue);
                }
            } else {
                newValue = targetValue;
            }
        }

        long duration = currentTime - startTime;

        if ((fadeTime != 0) && (newValue != targetValue)) {
            // calculate new fade value
            if (stepDuration == 0) {
                stepDuration = 1;
            }
            int currentStep = (int) (duration / stepDuration);

            if (fadeDirection == FadeDirection.up) {
                newValue = startValue + currentStep;
                if (newValue > targetValue) {
                    newValue = targetValue;
                }
            } else {
                newValue = startValue - currentStep;
                if (newValue < targetValue) {
                    newValue = targetValue;
                }
            }
        }

        if (newValue == targetValue && holdTime > -1) {
            // we reached the target already, check if we need to hold longer
            if (((holdTime > 0 || fadeTime > 0) && (duration >= fadeTime + holdTime))
                    || (holdTime == 0 && fadeTime == 0)) {
                // mark action as completed
                completed = true;
            }
        }

        return newValue;
    }

    @Override
    public String toString() {
        return "FadeAction: " + String.valueOf(targetValue) + ", fade time " + String.valueOf(fadeTime)
                + "ms, hold time " + String.valueOf(holdTime) + "ms";
    }
}
