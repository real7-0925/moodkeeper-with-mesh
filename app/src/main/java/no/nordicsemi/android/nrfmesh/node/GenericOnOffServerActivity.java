package no.nordicsemi.android.nrfmesh.node;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.util.Random;

import androidx.annotation.NonNull;
import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.models.GenericOnOffServerModel;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.GenericOnOffGet;
import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.utils.MeshAddress;
import no.nordicsemi.android.mesh.utils.MeshParserUtils;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.databinding.LayoutGenericOnOffBinding;

@AndroidEntryPoint
public class GenericOnOffServerActivity extends ModelConfigurationActivity {

    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();

    private TextView onOffState;
    private TextView remainingTime;
    private Button mActionOnOff;
    protected int mTransitionStepResolution;
    protected int mTransitionSteps;

    byte value_R, value_G, value_B;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwipe.setOnRefreshListener(this);
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (model instanceof GenericOnOffServerModel) {
            final LayoutGenericOnOffBinding nodeControlsContainer = LayoutGenericOnOffBinding.inflate(getLayoutInflater(), binding.nodeControlsContainer, true);
            final TextView time = nodeControlsContainer.transitionTime;
            onOffState = nodeControlsContainer.onOffState;
            remainingTime = nodeControlsContainer.transitionState;
            final Slider transitionTimeSlider = nodeControlsContainer.transitionSlider;
            transitionTimeSlider.setValueFrom(0);
            transitionTimeSlider.setValueTo(230);
            transitionTimeSlider.setValue(0);
            transitionTimeSlider.setStepSize(1);

            final Slider delaySlider = nodeControlsContainer.delaySlider;
            delaySlider.setValueFrom(0);
            delaySlider.setValueTo(255);
            delaySlider.setValue(0);
            delaySlider.setStepSize(1);
            final TextView delayTime = nodeControlsContainer.delayTime;

            mActionOnOff = nodeControlsContainer.actionOn;
            mActionOnOff.setOnClickListener(v -> {
                try {
                    sendGenericOnOff(mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on)), (int) delaySlider.getValue());
                } catch (IllegalArgumentException ex) {
                    mViewModel.displaySnackBar(this, mContainer, ex.getMessage(), Snackbar.LENGTH_LONG);
                }
            });

            //alan_add
            Slider slider1 = (Slider) findViewById(R.id.seekbar_Red);
            Slider slider2 = (Slider) findViewById(R.id.seekbar_Green);
            Slider slider3 = (Slider) findViewById(R.id.seekbar_Blue);

//        slider1.showContextMenu();// 设置推动时显示指示器
//        slider2.showContextMenu();
//        slider3.showContextMenu();

            slider1.setTrackTintList(ColorStateList.valueOf(0xFF881515));// 滑軌底色
            slider2.setTrackTintList(ColorStateList.valueOf(0xFF308014));
            slider3.setTrackTintList(ColorStateList.valueOf(0xFF3D59AB));

            slider1.setValue(0);// 设定初始进度
            slider2.setValue(0);
            slider3.setValue(0);

            slider1.setValueFrom(0);
            slider2.setValueFrom(0);
            slider3.setValueFrom(0);

            slider1.setValueTo(100);// 设定最终进度
            slider2.setValueTo(100);
            slider3.setValueTo(100);

            slider1.setStepSize(1);
            slider2.setStepSize(1);
            slider3.setStepSize(1);

            slider1.setHaloTintList(ColorStateList.valueOf(0xFFFF0000));// 设定光環顔色
            slider2.setHaloTintList(ColorStateList.valueOf(0xFF00FF00));
            slider3.setHaloTintList(ColorStateList.valueOf(0xFF0000FF));

            slider1.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));// 设定滑塊顔色
            slider2.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));
            slider3.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));

            slider1.setTrackActiveTintList(ColorStateList.valueOf(0xFFE3170D));//設置軌道活動部分顔色
            slider2.setTrackActiveTintList(ColorStateList.valueOf(0xFF32CD32));
            slider3.setTrackActiveTintList(ColorStateList.valueOf(0xFF1E90FF));

            slider1.setTrackHeight(20);//設置軌道寬度
            slider2.setTrackHeight(20);
            slider3.setTrackHeight(20);
//        slider1.setBackgroundColor(getResources().getColor(R.color.red));// 背景颜色// 监听进度
//        slider1.setOnValueChangedListener(new OnValueChangedListener() {
//
//            @Override
//            public void onValueChanged(int value) {
//                // TODO 自动生成的方法存根
//                System.out.println("now value = "+ value);
//            }
//        });

            slider1.setThumbElevation(30);// 设置滑块的影子大小
            slider2.setThumbElevation(30);// 设置滑块的影子大小
            slider3.setThumbElevation(30);// 设置滑块的影子大小

            /*add by myself*/
            final byte[] b = new byte[101];
            for(int count = 0;count <= 100;count++){
                b[count] = (byte)(count);
            };


            mActionRead = nodeControlsContainer.actionRead;
            mActionRead.setOnClickListener(v -> sendGenericOnOffGet());

            /*add by myself*/
            slider1.addOnChangeListener(new Slider.OnChangeListener() {
                int lastValue = 0;
//                double res = 0.0;

                @Override
                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                    final int progress1 = (int) value;
                    lastValue = progress1;
                    mTransitionStepResolution = progress1-1;
                    mTransitionSteps = progress1;
                    value_R=b[progress1];
                }
            });

            slider2.addOnChangeListener(new Slider.OnChangeListener() {
                int lastValue2 = 0;
//                double res = 0.0;

                @Override
                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                    final int progress2 = (int) value;
                    lastValue2 = progress2;
                    mTransitionStepResolution = progress2-1;
                    mTransitionSteps = progress2;
                    value_G=b[progress2];
                }
            });

            slider3.addOnChangeListener(new Slider.OnChangeListener() {
                int lastValue3 = 0;
//                double res = 0.0;

                @Override
                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                    final int progress3 = (int) value;
                    lastValue3 = progress3;
                    mTransitionStepResolution = progress3-1;
                    mTransitionSteps = progress3;
                    value_B=b[progress3];
                }
            });


//            transitionTimeSlider.addOnChangeListener(new Slider.OnChangeListener() {
//                int lastValue = 0;
//                double res = 0.0;

//                @Override
//                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
//                    final int progress = (int) value;
//                    if (progress >= 0 && progress <= 62) {
//                        lastValue = progress;
//                        mTransitionStepResolution = 0;
//                        mTransitionSteps = progress;
//                        res = progress / 10.0;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(res), "s"));
//                    } else if (progress >= 63 && progress <= 118) {
//                        if (progress > lastValue) {
//                            mTransitionSteps = progress - 56;
//                            lastValue = progress;
//                        } else if (progress < lastValue) {
//                            mTransitionSteps = -(56 - progress);
//                        }
//                        mTransitionStepResolution = 1;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps), "s"));
//
//                    } else if (progress >= 119 && progress <= 174) {
//                        if (progress > lastValue) {
//                            mTransitionSteps = progress - 112;
//                            lastValue = progress;
//                        } else if (progress < lastValue) {
//                            mTransitionSteps = -(112 - progress);
//                        }
//                        mTransitionStepResolution = 2;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "s"));
//                    } else if (progress >= 175 && progress <= 230) {
//                        if (progress >= lastValue) {
//                            mTransitionSteps = progress - 168;
//                            lastValue = progress;
//                        } else {
//                            mTransitionSteps = -(168 - progress);
//                        }
//                        mTransitionStepResolution = 3;
//                        time.setText(getString(R.string.transition_time_interval, String.valueOf(mTransitionSteps * 10), "min"));
//                    }
//                }

            delaySlider.addOnChangeListener((slider, value, fromUser) -> delayTime.setText(getString(R.string.transition_time_interval, String.valueOf((int) value * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms")));

            mViewModel.getSelectedModel().observe(this, meshModel -> {
                if (meshModel != null) {
                    updateAppStatusUi(meshModel);
                    updatePublicationUi(meshModel);
                    updateSubscriptionUi(meshModel);
                }
            });
        }
    }

    @Override
    protected void enableClickableViews() {
        super.enableClickableViews();
        if (mActionOnOff != null && !mActionOnOff.isEnabled())
            mActionOnOff.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        super.disableClickableViews();
        if (mActionOnOff != null)
            mActionOnOff.setEnabled(false);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);
        mSwipe.setOnRefreshListener(this);
        if (meshMessage instanceof GenericOnOffStatus) {
            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
            final boolean presentState = status.getPresentState();
            final Boolean targetOnOff = status.getTargetState();
            final int steps = status.getTransitionSteps();
            final int resolution = status.getTransitionResolution();
            if (targetOnOff == null) {
                if (presentState) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setVisibility(View.GONE);
            } else {
                if (!targetOnOff) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setText(getString(R.string.remaining_time, MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
                remainingTime.setVisibility(View.VISIBLE);
            }
        }
        hideProgressBar();
    }

    /**
     * Send generic on off get to mesh node
     */
    public void sendGenericOnOffGet() {
        if (!checkConnectivity(mContainer)) return;
        final Element element = mViewModel.getSelectedElement().getValue();
        if (element != null) {
            final MeshModel model = mViewModel.getSelectedModel().getValue();
            if (model != null) {
                if (!model.getBoundAppKeyIndexes().isEmpty()) {
                    final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                    final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);

                    final int address = element.getElementAddress();
                    Log.v(TAG, "Sending message to element's unicast address: " + MeshAddress.formatAddress(address, true));

                    final GenericOnOffGet genericOnOffSet = new GenericOnOffGet(appKey);
                    sendMessage(address, genericOnOffSet);
                } else {
                    mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    /**
     * Send generic on off set to mesh node
     *
     * @param state true to turn on and false to turn off
     * @param delay message execution delay in 5ms steps. After this delay milliseconds the model will execute the required behaviour.
     */
    public void sendGenericOnOff(final boolean state, final Integer delay) {
        if (!checkConnectivity(mContainer)) return;
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        if (node != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    if (!model.getBoundAppKeyIndexes().isEmpty()) {
                        final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                        final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
                        final int address = element.getElementAddress();
                        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(appKey, state,
                                new Random().nextInt(), mTransitionSteps, mTransitionStepResolution, delay);
                        sendMessage(address, genericOnOffSet);
                    } else {
                        mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                    }
                }
            }
        }
    }
}
