package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import androidx.annotation.NonNull;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;

public abstract class GarminCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public String getManufacturer() {
        return "Garmin";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return GarminSupport.class;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_high_mtu);

        if (getCannedRepliesSlotCount(device) > 0) {
            final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);

            notifications.add(R.xml.devicesettings_garmin_default_reply_suffix);
            notifications.add(R.xml.devicesettings_canned_reply_16);
            notifications.add(R.xml.devicesettings_canned_dismisscall_16);
        }

        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_keep_activity_data_on_device);

        return deviceSpecificSettings;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }
}