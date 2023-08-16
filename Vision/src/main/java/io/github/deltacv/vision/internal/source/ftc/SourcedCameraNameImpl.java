package io.github.deltacv.vision.internal.source.ftc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.qualcomm.robotcore.util.SerialNumber;
import io.github.deltacv.vision.external.source.VisionSource;
import org.jetbrains.annotations.NotNull;

public class SourcedCameraNameImpl extends SourcedCameraName {

    private VisionSource source;

    public SourcedCameraNameImpl(VisionSource source) {
        this.source = source;
    }

    @Override
    public VisionSource getSource() {
        return source;
    }

    @Override
    public Manufacturer getManufacturer() {
        return null;
    }

    @Override
    public String getDeviceName() {
        return null;
    }

    @Override
    public String getConnectionInfo() {
        return null;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {

    }

    @Override
    public void close() {

    }

    @NonNull
    @NotNull
    @Override
    public SerialNumber getSerialNumber() {
        return null;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public String getUsbDeviceNameIfAttached() {
        return null;
    }

    @Override
    public boolean isAttached() {
        return false;
    }
}
