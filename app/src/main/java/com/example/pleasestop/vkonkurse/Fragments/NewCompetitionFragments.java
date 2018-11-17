package com.example.pleasestop.vkonkurse.Fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.example.pleasestop.vkonkurse.Adapters.MyAdapter;
import com.example.pleasestop.vkonkurse.MyApp;
import com.example.pleasestop.vkonkurse.MyForeGroundService;
import com.example.pleasestop.vkonkurse.model.Competition;
import com.example.pleasestop.vkonkurse.presenters.NewCompetitionPresenter;
import com.example.pleasestop.vkonkurse.R;
import com.example.pleasestop.vkonkurse.Repository;
import com.example.pleasestop.vkonkurse.ViewsMvp.NewCompetitionView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Все новые конкурсы будут появляться здесь
 */
public class NewCompetitionFragments extends MvpAppCompatFragment implements NewCompetitionView {
    MyAdapter<Competition, NewCompetitionPresenter> adapter;

    @Inject
    Repository repository;

    @Inject
    SharedPreferences preferences;

    @InjectPresenter
    NewCompetitionPresenter presenter;

    @ProvidePresenter
    NewCompetitionPresenter getPresenter(){
        return new NewCompetitionPresenter();
    }

    @BindView(R.id.error_message)
    TextView errorMessege;
    @BindView(R.id.loading_progress)
    ProgressBar progressBar;
    @BindView(R.id.switchService)
    Switch swichService;
    @BindView(R.id.recycler_view)
    RecyclerView list;

    @OnClick(R.id.switchService)
    public void click(){
        if(!swichService.getSplitTrack()){
            swichService.setSplitTrack(true);
            startService();
        } else {
            swichService.setSplitTrack(false);
            stopService();
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_compitations_fragment, container, false);
        MyApp.getNetComponent().inject(this);
        ButterKnife.bind(this, view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(linearLayoutManager);
        list.setHasFixedSize(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(list.getContext(), linearLayoutManager.getOrientation());
        list.addItemDecoration(dividerItemDecoration);
        adapter = new MyAdapter<>(R.layout.item_new_compitation, presenter);
        list.setAdapter(adapter);

        return view;
    }


    @Override
    public void startService(){
        String input = "test";
        Intent serviceIntent = new Intent(getActivity(), MyForeGroundService.class);
        serviceIntent.putExtra("inputExtra", input);
        ContextCompat.startForegroundService(getActivity(), serviceIntent);
    }


    @Override
    public void stopService() {
        Intent serviceIntent = new Intent(getActivity(), MyForeGroundService.class);
        getActivity().stopService(serviceIntent);
    }

    @Override
    public void addList(List<Competition> list) {
        adapter.addAll(list);
    }

    @Override
    public void showError(String error) {
        errorMessege.setText(error);
    }

    @Override
    public void loading(boolean visible) {
        if(visible){
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
}
