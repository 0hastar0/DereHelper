package com.lazyeraser.imas.cgss.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;

import com.kelin.mvvmlight.messenger.Messenger;
import com.lazyeraser.imas.cgss.utils.SharedHelper;
import com.lazyeraser.imas.cgss.utils.UpdateManager;
import com.lazyeraser.imas.cgss.view.fragments.AboutFrag;
import com.lazyeraser.imas.cgss.view.fragments.CardListFrag;
import com.lazyeraser.imas.cgss.view.fragments.CharaListFrag;
import com.lazyeraser.imas.cgss.view.fragments.SongListFrag;
import com.lazyeraser.imas.cgss.viewmodel.MainViewModel;
import com.lazyeraser.imas.derehelper.R;
import com.lazyeraser.imas.main.BaseActivity;
import com.lazyeraser.imas.main.BaseFragment;
import com.lazyeraser.imas.main.SStaticR;


import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by lazyeraser on 2017/9/18.
 */

public class MainActivity extends BaseActivity {

    private SweetAlertDialog upToDateDialog, haveUpdateDialog;
    private boolean manualCheck = false;
    private SweetAlertDialog progressDialog;
    private DrawerLayout drawerLayout;

    private CardListFrag cardListFrag;
    private CharaListFrag charaListFrag;
    private SongListFrag songListFrag;
    private AboutFrag aboutFrag;


    private Map<BaseFragment, Boolean> fragments;

    private UpdateManager updateManager;
    public final static int TOKEN_DATA_UPDATED = 0x12450;
    private boolean needUpdateHint;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(R.string.card_list);
        initActionBar(ACTIONBAR_TYPE_CUSTOM, R.drawable.umi_ic_menu_white, R.drawable.ic_filter_list_white_24dp);
        actionBarStartAction(menu -> umi.moveDrawer(drawerLayout, Gravity.START));

        MainViewModel mainViewModel = new MainViewModel(this);
        setBinding(R.layout.activity_main).setVariable(com.lazyeraser.imas.derehelper.BR.viewModel, mainViewModel);
        updateManager = new UpdateManager(mContext);
        drawerLayout = (DrawerLayout)getBView(R.id.drawerLayout);
        NavigationView navigationView = (NavigationView)getBView(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            switch (id){
                case R.id.nav_cards:
                    switchFrag(cardListFrag);
                    break;
                case R.id.nav_idols:
                    switchFrag(charaListFrag);
                    break;
                case R.id.nav_check_update:
                    manualCheck = true;
                    mainViewModel.checkDataUpdate();
                    break;
                case R.id.nav_check_update_app:
                    checkUpdate(true);
                    break;
                case R.id.nav_settings:
                    umi.jumpTo(SettingsActivity.class);
                    break;
                case R.id.nav_about:
                    switchFrag(aboutFrag);
                    break;
                case R.id.nav_song:
                    switchFrag(songListFrag);
                    break;
            }
            umi.moveDrawer(drawerLayout, Gravity.START);
            return true;
        });

        initDialog(mainViewModel); // 初始化数据更新对话框

        fragments = new HashMap<>();
        cardListFrag = new CardListFrag();
        charaListFrag = new CharaListFrag();
        songListFrag = new SongListFrag();
        aboutFrag = new AboutFrag();

        fragments.put(cardListFrag, false);
        fragments.put(charaListFrag, false);
        fragments.put(songListFrag, false);
        fragments.put(aboutFrag, false);

        switchFrag(cardListFrag);
        if (umi.getSP(SharedHelper.KEY_AUTO_APP)){
            checkUpdate(false);
        }
        // 监听语言设置改变
        if (broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg1, Intent arg2) {
                    if (arg2.getAction().equals(Intent.ACTION_LOCALE_CHANGED)){
                        askRestart();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
            intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
            registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void askRestart(){
        SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.locale_changed))
                .setContentText(getString(R.string.locale_changed_need_restart))
                .setConfirmText("OK")
                .setConfirmClickListener(dialog -> {
                    Intent intent = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    PendingIntent restartIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                    if(Build.VERSION.SDK_INT < 19){
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, restartIntent);
                    }else{
                        mgr.setExact(AlarmManager.RTC, System.currentTimeMillis() + 500, restartIntent);
                    }
                    if (SStaticR.ANALYTICS_ON){

                    }
                    System.exit(0);
                });
        sweetAlertDialog.setCancelable(false);
        sweetAlertDialog.show();
    }

    private void checkUpdate(boolean hint){
        needUpdateHint = hint;
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }else {
            updateManager.checkUpdate(hint);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                updateManager.checkUpdate(needUpdateHint);
            }else {
                umi.makeToast(R.string.permission_denied_hint);
            }
        }
    }

    private void initDialog(MainViewModel mainViewModel){
        upToDateDialog = new SweetAlertDialog(mContext, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(getString(R.string.check_hint))
                .setConfirmClickListener(Dialog::dismiss);
        haveUpdateDialog = new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.update_hint0))
                .setContentText(getString(R.string.update_hint1))
                .setCancelText(getString(R.string.update_hint2))
                .setConfirmText(getString(R.string.update_hint3))
                .setCancelClickListener(dialog ->{
                    dialog.dismiss();
                    mainViewModel.haveUpdate.set(false);
                    Messenger.getDefault().sendNoMsg(TOKEN_DATA_UPDATED);
                })
                .setConfirmClickListener(alertDialog -> {
                    alertDialog.dismiss();
                    mainViewModel.agree.set(true);
                });
        progressDialog = new SweetAlertDialog(mContext, SweetAlertDialog.PROGRESS_TYPE)
                .setTitleText(getString(R.string.update_hint4))
                .setContentText(getString(R.string.update_hint5));
//        progressDialog.getProgressHelper().setProgress(0);
        progressDialog.setCancelable(false);


        mainViewModel.upToDate.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (mainViewModel.upToDate.get()){
                    if (manualCheck){
                        umi.dismissLoading();
                        upToDateDialog.show();
                    }else {
                        Messenger.getDefault().sendNoMsg(TOKEN_DATA_UPDATED);
                    }
                    mainViewModel.upToDate.set(false);
                    manualCheck = false;
                }
            }
        });
        mainViewModel.haveUpdate.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean)observable).get() && !haveUpdateDialog.isShowing()){
                    haveUpdateDialog.show();
                }
            }
        });
        mainViewModel.isShowProgress.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean)observable).get()){
                    progressDialog.show();
                }else {
                    progressDialog.dismissWithAnimation();
                }
            }
        });
        mainViewModel.progress.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                float progess = ((ObservableFloat)observable).get();
                progressDialog.getProgressHelper().setInstantProgress(progess);
                if (progess >= 1){
                    progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    progressDialog.setTitleText(getString(R.string.update_hint_finish));
                    progressDialog.setConfirmText("OK");
                    progressDialog.setConfirmClickListener(sweetAlertDialog -> {
                        Messenger.getDefault().sendNoMsg(TOKEN_DATA_UPDATED);
                        sweetAlertDialog.dismiss();
                    });
                }
            }
        });
        mainViewModel.progressTxt.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onPropertyChanged(Observable observable, int i) {
                progressDialog.setContentText(((ObservableField<String>)observable).get());
            }
        });
    }


    private void switchFrag(BaseFragment fragment){
        setActionBarTitleAgain(fragment.getTitle());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!fragments.get(fragment)){
            transaction.add(R.id.main_content, fragment);
            fragments.put(fragment, true);
        }
        for (BaseFragment baseFragment : fragments.keySet()) {
            if (baseFragment != null) transaction.hide(baseFragment);
        }
        transaction.show(fragment);
        transaction.commit();
        View.OnClickListener action_end = fragment.getMenuAction_end();
        if (action_end != null){
            initActionBar(ACTIONBAR_TYPE_CUSTOM, R.drawable.umi_ic_menu_white, R.drawable.ic_filter_list_white_24dp);
            actionBarEndAction(action_end);
        }else {
            initActionBar(ACTIONBAR_TYPE_CUSTOM, R.drawable.umi_ic_menu_white, null);
        }
    }

    private long exitTime;

    @Override
    protected void backBtnAction() {
        if (drawerLayout.isDrawerOpen(Gravity.START)){
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            exitTime = System.currentTimeMillis();
            umi.makeSBar(R.string.app_exit_hint, drawerLayout);
        } else {
            umi.destroyAllActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }

    }
}
