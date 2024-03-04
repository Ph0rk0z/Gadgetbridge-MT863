/*  Copyright (C) 2020-2024 Andreas Shimokawa, José Rebelo, Sebastian Krey,
    Your Name

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSportsActivityType;

public class HuamiActivitySummaryParser implements ActivitySummaryParser {

    private static final Logger LOG = LoggerFactory.getLogger(HuamiActivityDetailsParser.class);
    private JSONObject summaryData = new JSONObject();


    public BaseActivitySummary parseBinaryData(BaseActivitySummary summary) {
        Date startTime = summary.getStartTime();
        if (startTime == null) {
            LOG.error("Due to a bug, we can only parse the summary when startTime is already set");
            return null;
        }
        summaryData = new JSONObject();
        parseBinaryData(summary, startTime);
        summary.setSummaryData(summaryData.toString());
        return summary;
    }

    public AbstractHuamiActivityDetailsParser getDetailsParser(final BaseActivitySummary summary) {
        return new HuamiActivityDetailsParser(summary);
    }

    protected void parseBinaryData(BaseActivitySummary summary, Date startTime) {
        ByteBuffer buffer = ByteBuffer.wrap(summary.getRawSummaryData()).order(ByteOrder.LITTLE_ENDIAN);

        short version = buffer.getShort(); // version
        LOG.debug("Got sport summary version " + version + " total bytes=" + buffer.capacity());
        int activityKind = ActivityKind.TYPE_UNKNOWN;
        int rawKind = BLETypeConversions.toUnsigned(buffer.getShort());
        try {
            HuamiSportsActivityType activityType = HuamiSportsActivityType.fromCode(rawKind);
            activityKind = activityType.toActivityKind();
        } catch (Exception ex) {
            LOG.error("Error mapping activity kind: " + ex.getMessage(), ex);
            addSummaryData("Raw Activity Kind", rawKind, UNIT_NONE);
        }
        summary.setActivityKind(activityKind);

        // FIXME: should honor timezone we were in at that time etc
        long timestamp_start = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;
        long timestamp_end = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;


        // FIXME: should be done like this but seems to return crap when in DST
        //summary.setStartTime(new Date(timestamp_start));
        //summary.setEndTime(new Date(timestamp_end));

        // FIXME ... so do it like this
        long duration = timestamp_end - timestamp_start;
        summary.setEndTime(new Date(startTime.getTime() + duration));

        int baseLongitude = buffer.getInt();
        int baseLatitude = buffer.getInt();
        int baseAltitude = buffer.getInt();
        summary.setBaseLongitude(baseLongitude);
        summary.setBaseLatitude(baseLatitude);
        summary.setBaseAltitude(baseAltitude);

        int steps;
        int activeSeconds;
        int maxLatitude;
        int minLatitude;
        int maxLongitude;
        int minLongitude;
        float caloriesBurnt;
        float distanceMeters;
        float distanceMeters2 = 0;
        float ascentDistance = 0;
        float descentDistance = 0;
        float flatDistance = 0;
        float ascentMeters = 0;
        float descentMeters = 0;
        float maxAltitude = 0;
        float minAltitude = 0;
        float averageAltitude = 0;
        float maxSpeed = 0;
        float minSpeed = 0;
        float averageSpeed = 0;
        float minPace = 0;
        float maxPace = 0;
        float averagePace = 0;
        int maxCadence = 0;
        int minCadence = 0;
        int averageCadence = 0;
        int maxStride = 0;
        int minStride = 0;
        int averageStride2 = 0;
        float totalStride = 0;
        float averageStride;
        short averageHR;
        short maxHR = 0;
        short minHR = 0;
        short averageKMPaceSeconds;
        int ascentSeconds = 0;
        int descentSeconds = 0;
        int flatSeconds = 0;

        // Swimming
        float averageStrokeDistance = 0;
        float averageStrokesPerSecond = 0;
        float averageLapPace = 0;
        short strokes = 0;
        short swolfIndex = 0; // this is called SWOLF score on bip s, SWOLF index on mi band 4
        byte swimStyle = 0;
        byte laps = 0;

        // Just assuming, Bip has 259 which seems like 256+x
        // Bip S now has 518 so assuming 512+x, might be wrong

        if (version >= 512) {
            if (version == 519) {
                buffer.get(); // skip one byte
                minHR  = buffer.getShort();
                // hack that skips data on yet unknown summary version 519 data
                buffer.position(0x8c);
            } else if (version == 516) {
                // hack that skips data on yet unknown summary version 516 data
                buffer.position(buffer.position() + 4);
            }
            steps = buffer.getInt();
            activeSeconds = buffer.getInt();

            maxLatitude = buffer.getInt();
            minLatitude = buffer.getInt();
            maxLongitude = buffer.getInt();
            minLongitude = buffer.getInt();

            caloriesBurnt = buffer.getFloat();
            distanceMeters = buffer.getFloat();

            ascentMeters = buffer.getFloat();
            descentMeters = buffer.getFloat();
            maxAltitude = buffer.getFloat();
            minAltitude = buffer.getFloat();
            averageAltitude = buffer.getFloat();

            maxSpeed = buffer.getFloat(); // in meter/second
            minSpeed = buffer.getFloat();
            averageSpeed = buffer.getFloat();
            minPace = buffer.getFloat(); // in seconds/meter
            maxPace = buffer.getFloat();
            averagePace = buffer.getFloat();

            maxCadence = Math.round(buffer.getFloat() * 60);
            minCadence = Math.round(buffer.getFloat() * 60);
            averageCadence = Math.round(buffer.getFloat() * 60);

            maxStride = Math.round(buffer.getFloat() * 100);
            minStride = Math.round(buffer.getFloat() * 100);
            averageStride2 = Math.round(buffer.getFloat() * 100);

            distanceMeters2 = buffer.getFloat(); // this distance is 87-97% of distanceMeters, so probably length of the GPS track (difference is larger, when GPS took longer to get a precise position)
            buffer.getInt();
            averageHR = buffer.getShort();
            averageKMPaceSeconds = buffer.getShort();
            averageStride = buffer.getShort();
            maxHR = buffer.getShort();

            if (activityKind == ActivityKind.TYPE_CYCLING || activityKind == ActivityKind.TYPE_RUNNING || activityKind == ActivityKind.TYPE_HIKING || activityKind == ActivityKind.TYPE_CLIMBING) {
                // this had nonsense data with treadmill on bip s, need to test it with running
                // for cycling it seems to work... hmm...
                // 28 bytes
                buffer.getInt(); // unknown
                ascentDistance = buffer.getFloat();
                ascentSeconds = buffer.getInt() / 1000; //ms?
                descentDistance = buffer.getFloat();
                descentSeconds = buffer.getInt() / 1000; //ms?
                flatDistance = buffer.getFloat();
                flatSeconds = buffer.getInt() / 1000; // ms?
            } else if (activityKind == ActivityKind.TYPE_SWIMMING || activityKind == ActivityKind.TYPE_SWIMMING_OPENWATER) {
                // offset 0x8c
                /*
                    data on the bip s display (example)
                    main style backstroke
                    SWOLF score 92
                    total laps 1
                    avg. pace 2,09/100
                    strokes 36
                    avg stroke rate 26 spm
                    single stroke distance 1,79m
                    max stroke rate 39
                 */

                averageStrokeDistance = buffer.getFloat();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                averageStrokesPerSecond = buffer.getFloat();
                averageLapPace = buffer.getFloat();
                buffer.getInt(); // unknown
                strokes = buffer.getShort();
                swolfIndex = buffer.getShort();
                swimStyle = buffer.get();
                laps = buffer.get();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
            }
        } else {
            distanceMeters = buffer.getFloat();
            ascentMeters = buffer.getFloat();
            descentMeters = buffer.getFloat();
            minAltitude = buffer.getFloat();
            maxAltitude = buffer.getFloat();
            maxLatitude = buffer.getInt(); // format?
            minLatitude = buffer.getInt(); // format?
            maxLongitude = buffer.getInt(); // format?
            minLongitude = buffer.getInt(); // format?
            steps = buffer.getInt();
            activeSeconds = buffer.getInt();
            caloriesBurnt = buffer.getFloat();
            maxSpeed = buffer.getFloat();
            maxPace = buffer.getFloat();
            minPace = buffer.getFloat();
            totalStride = buffer.getFloat();

            buffer.getInt(); // unknown
            if (activityKind == ActivityKind.TYPE_SWIMMING) {
                // 28 bytes
                averageStrokeDistance = buffer.getFloat();
                averageStrokesPerSecond = buffer.getFloat();
                averageLapPace = buffer.getFloat();
                strokes = buffer.getShort();
                swolfIndex = buffer.getShort();
                swimStyle = buffer.get();
                laps = buffer.get();
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown
                buffer.getShort(); // unknown
            } else {
                // 28 bytes
                buffer.getInt(); // unknown
                buffer.getInt(); // unknown probably ascentDistance = buffer.getFloat();
                ascentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown probably descentDistance = buffer.getFloat();
                descentSeconds = buffer.getInt() / 1000; //ms?
                buffer.getInt(); // unknown probably flatDistance = buffer.getFloat();
                flatSeconds = buffer.getInt() / 1000; // ms?

                addSummaryData(ASCENT_SECONDS, ascentSeconds, UNIT_SECONDS);
                addSummaryData(DESCENT_SECONDS, descentSeconds, UNIT_SECONDS);
                addSummaryData(FLAT_SECONDS, flatSeconds, UNIT_SECONDS);
            }

            averageHR = buffer.getShort();

            averageKMPaceSeconds = buffer.getShort();
            averageStride = buffer.getShort();
        }

//        summary.setBaseCoordinate(new GPSCoordinate(baseLatitude, baseLongitude, baseAltitude));
//        summary.setDistanceMeters(distanceMeters);
//        summary.setAscentMeters(ascentMeters);
//        summary.setDescentMeters(descentMeters);
//        summary.setMinAltitude(maxAltitude);
//        summary.setMaxAltitude(maxAltitude);
//        summary.setMinLatitude(minLatitude);
//        summary.setMaxLatitude(maxLatitude);
//        summary.setMinLongitude(minLatitude);
//        summary.setMaxLongitude(maxLatitude);
//        summary.setSteps(steps);
//        summary.setActiveTimeSeconds(secondsActive);
//        summary.setCaloriesBurnt(caloriesBurnt);
//        summary.setMaxSpeed(maxSpeed);
//        summary.setMinPace(minPace);
//        summary.setMaxPace(maxPace);
//        summary.setTotalStride(totalStride);
//        summary.setTimeAscent(BLETypeConversions.toUnsigned(ascentSeconds);
//        summary.setTimeDescent(BLETypeConversions.toUnsigned(descentSeconds);
//        summary.setTimeFlat(BLETypeConversions.toUnsigned(flatSeconds);
//        summary.setAverageHR(BLETypeConversions.toUnsigned(averageHR);
//        summary.setAveragePace(BLETypeConversions.toUnsigned(averagePace);
//        summary.setAverageStride(BLETypeConversions.toUnsigned(averageStride);

        addSummaryData(ASCENT_SECONDS, ascentSeconds, UNIT_SECONDS);
        addSummaryData(DESCENT_SECONDS, descentSeconds, UNIT_SECONDS);
        addSummaryData(FLAT_SECONDS, flatSeconds, UNIT_SECONDS);
        addSummaryData(ASCENT_DISTANCE, ascentDistance, UNIT_METERS);
        addSummaryData(DESCENT_DISTANCE, descentDistance, UNIT_METERS);
        addSummaryData(FLAT_DISTANCE, flatDistance, UNIT_METERS);

        addSummaryData(DISTANCE_METERS, distanceMeters, UNIT_METERS);
        // addSummaryData("distanceMeters2", distanceMeters2, UNIT_METERS);
        addSummaryData(ASCENT_METERS, ascentMeters, UNIT_METERS);
        addSummaryData(DESCENT_METERS, descentMeters, UNIT_METERS);
        if (maxAltitude != -100000) {
            addSummaryData(ALTITUDE_MAX, maxAltitude, UNIT_METERS);
        }
        if (minAltitude != 100000) {
            addSummaryData(ALTITUDE_MIN, minAltitude, UNIT_METERS);
        }
        if (minAltitude != 100000) {
            addSummaryData(ALTITUDE_AVG, averageAltitude, UNIT_METERS);
        }
        addSummaryData(STEPS, steps, UNIT_STEPS);
        addSummaryData(ACTIVE_SECONDS, activeSeconds, UNIT_SECONDS);
        addSummaryData(CALORIES_BURNT, caloriesBurnt, UNIT_KCAL);
        addSummaryData(SPEED_MAX, maxSpeed, UNIT_METERS_PER_SECOND);
        addSummaryData(SPEED_MIN, minSpeed, UNIT_METERS_PER_SECOND);
        addSummaryData(SPEED_AVG, averageSpeed, UNIT_METERS_PER_SECOND);
        addSummaryData(CADENCE_MAX, maxCadence, UNIT_SPM);
        addSummaryData(CADENCE_MIN, minCadence, UNIT_SPM);
        addSummaryData(CADENCE_AVG, averageCadence, UNIT_SPM);

        if (!(activityKind == ActivityKind.TYPE_ELLIPTICAL_TRAINER ||
                activityKind == ActivityKind.TYPE_JUMP_ROPING ||
                activityKind == ActivityKind.TYPE_EXERCISE ||
                activityKind == ActivityKind.TYPE_YOGA ||
                activityKind == ActivityKind.TYPE_INDOOR_CYCLING)) {
            addSummaryData(PACE_MIN, minPace, UNIT_SECONDS_PER_M);
            addSummaryData(PACE_MAX, maxPace, UNIT_SECONDS_PER_M);
            // addSummaryData("averagePace", averagePace, UNIT_SECONDS_PER_M);
        }

        addSummaryData(STRIDE_TOTAL, totalStride, UNIT_METERS);
        addSummaryData(HR_AVG, averageHR, UNIT_BPM);
        addSummaryData(HR_MAX, maxHR, UNIT_BPM);
        addSummaryData(HR_MIN, minHR, UNIT_BPM);
        addSummaryData(PACE_AVG_SECONDS_KM, averageKMPaceSeconds, UNIT_SECONDS_PER_KM);
        addSummaryData(STRIDE_AVG, averageStride, UNIT_CM);
        addSummaryData(STRIDE_MAX, maxStride, UNIT_CM);
        addSummaryData(STRIDE_MIN, minStride, UNIT_CM);
        // addSummaryData("averageStride2", averageStride2, UNIT_CM);

        if (activityKind == ActivityKind.TYPE_SWIMMING || activityKind == ActivityKind.TYPE_SWIMMING_OPENWATER) {
            addSummaryData(STROKE_DISTANCE_AVG, averageStrokeDistance, UNIT_METERS);
            addSummaryData(STROKE_AVG_PER_SECOND, averageStrokesPerSecond, UNIT_STROKES_PER_SECOND);
            addSummaryData(LAP_PACE_AVERAGE, averageLapPace, "second");
            addSummaryData(STROKES, strokes, "strokes");
            addSummaryData(SWOLF_INDEX, swolfIndex, "swolf_index");
            String swimStyleName = "unknown"; // TODO: translate here or keep as string identifier here?
            switch (swimStyle) {
                case 1:
                    swimStyleName = "breaststroke";
                    break;
                case 2:
                    swimStyleName = "freestyle";
                    break;
                case 3:
                    swimStyleName = "backstroke";
                    break;
                case 4:
                    swimStyleName = "medley";
                    break;
            }
            addSummaryData(SWIM_STYLE, swimStyleName);
            addSummaryData(LAPS, laps, "laps");
        }
    }

    protected void addSummaryData(String key, float value, String unit) {
        if (value > 0) {
            try {
                JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", unit);
                summaryData.put(key, innerData);
            } catch (JSONException ignore) {
            }
        }
    }

    protected void addSummaryData(String key, String value) {
        if (key != null && !key.equals("") && value != null && !value.equals("")) {
            try {
                JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", "string");
                summaryData.put(key, innerData);
            } catch (JSONException ignore) {
            }
        }
    }
}
