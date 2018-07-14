/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.audio.player;

public class AudioLossCounter {

    public static final int EXPECTED_PACKET_COUNT_PER_MIN = (60 * 1000) / 20; // 20ms packets

    private long curMinute = 0;
    private int curLoss = 0;
    private int curSucc = 0;

    private int lastLoss = 0;
    private int lastSucc = 0;

    AudioLossCounter() {
    }

    void onLoss() {
        checkTime();
        curLoss++;
    }

    void onSuccess() {
        checkTime();
        curSucc++;
    }

    public int getLastMinuteLoss() {
        return lastLoss;
    }

    public int getLastMinuteSuccess() {
        return lastSucc;
    }

    private void checkTime() {
        long actualMinute = System.currentTimeMillis() / 60000;

        if(curMinute != actualMinute) {
            lastLoss = curLoss;
            lastSucc = curSucc;
            curLoss = 0;
            curSucc = 0;
            curMinute = actualMinute;
        }
    }

    @Override
    public String toString() {
        return "AudioLossCounter{" +
                "lastLoss=" + lastLoss +
                ", lastSucc=" + lastSucc +
                ", total=" + (lastSucc + lastLoss) +
                '}';
    }
}
