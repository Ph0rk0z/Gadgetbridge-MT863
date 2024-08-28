package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetAppNames;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendAppDelete;

public class HuaweiAppManager {

    static Logger LOG = LoggerFactory.getLogger(HuaweiAppManager.class);

    public static class AppConfig {

        public String bundleName;
        public String vendor;
        public String version;

        JSONObject distroFilters = null;

        public AppConfig(String jsonStr) {

            try {
                JSONObject config = new JSONObject(jsonStr);
                this.bundleName = config.getJSONObject("app").getString("bundleName");
                this.vendor = config.getJSONObject("app").getString("vendor");
                this.version = config.getJSONObject("app").getJSONObject("version").getString("name");

                parseDistroFilters(config);

            } catch (Exception e) {
                LOG.error("Error decode app config", e);
            }
        }

        private void parseDistroFilters(JSONObject config) {
            try {
                distroFilters = config.getJSONObject("module").getJSONObject("distroFilter");
            } catch (Exception e) {
                LOG.error("Error decode app config distroFilter", e);
            }
        }

        private boolean isValInArray(JSONArray arr, String value) throws JSONException {
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getString(i).equals(value))
                    return true;
            }
            return false;
        }

        public boolean checkDistroFilters(String screenShape, String screenWindow) {
            if (distroFilters == null)
                return false;
            try {
                boolean screenShapeSupported = false;
                boolean screenWindowSupported = false;
                if (distroFilters.has("screenShape")) {
                    JSONArray values = distroFilters.getJSONObject("screenShape").getJSONArray("value");
                    String policy = distroFilters.getJSONObject("screenShape").getString("policy");
                    screenShapeSupported = isValInArray(values, screenShape) && policy.equals("include");
                }
                if (distroFilters.has("screenWindow")) {
                    JSONArray values = distroFilters.getJSONObject("screenWindow").getJSONArray("value");
                    String policy = distroFilters.getJSONObject("screenWindow").getString("policy");
                    screenWindowSupported = isValInArray(values, screenWindow) && policy.equals("include");
                }
                return screenShapeSupported && screenWindowSupported;
            } catch (Exception e) {
                LOG.error("Error decode app config distroFilter", e);
                return false;
            }
        }

    }

    private final HuaweiSupportProvider support;

    private List<App.InstalledAppInfo> installedAppList = null;

    public HuaweiAppManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    public void setInstalledAppList(List<App.InstalledAppInfo> installedAppList) {
        this.installedAppList = installedAppList;
        handleAppList();
    }

    public void handleAppList() {
        if (this.installedAppList == null) {
            return;
        }

        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID uuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    uuid,
                    appInfo.appName,
                    "",
                    appInfo.version,
                    GBDeviceApp.Type.APP_GENERIC
            );
            gbDeviceApps.add(gbDeviceApp);
        }
        support.setGbWatchApps(gbDeviceApps);
    }

    public void requestAppList() {
        if (!this.support.getHuaweiCoordinator().supportsAppParams())
            return;
        try {
            GetAppNames getAppNames = new GetAppNames(support);
            getAppNames.doPerform();
        } catch (IOException e) {
            LOG.error("Error request applications list", e);
        }
    }

    public boolean startApp(UUID uuid) {
        if (this.installedAppList == null)
            return false;

        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID appUuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            if (appUuid.equals(uuid))
                return true;
        }
        return false;
    }

    public boolean deleteApp(UUID uuid) {
        if (this.installedAppList == null)
            return false;
        
        for (final App.InstalledAppInfo appInfo : installedAppList) {
            final UUID appUuid = UUID.nameUUIDFromBytes(appInfo.packageName.getBytes());
            if (appUuid.equals(uuid)) {
                try {
                    SendAppDelete sendAppDelete = new SendAppDelete(support, appInfo.packageName);
                    sendAppDelete.doPerform();
                } catch (IOException e) {
                    LOG.error("Could not delete app: " + appInfo.packageName, e);
                }
                return true;
            }
        }
        return false;
    }
}