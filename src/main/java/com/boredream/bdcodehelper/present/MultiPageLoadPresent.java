package com.boredream.bdcodehelper.present;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import com.boredream.bdcodehelper.R;
import com.boredream.bdcodehelper.adapter.LoadMoreAdapter;
import com.boredream.bdcodehelper.entity.ListResponse;
import com.boredream.bdcodehelper.entity.PageIndex;
import com.boredream.bdcodehelper.net.MultiPageRequest;
import com.boredream.bdcodehelper.net.ObservableDecorator;
import com.boredream.bdcodehelper.view.DividerItemDecoration;

import java.util.ArrayList;

import rx.Observable;
import rx.Subscriber;

public class MultiPageLoadPresent {

    private Activity activity;
    private LoadMoreAdapter loadMoreAdapter;

    private SwipeRefreshLayout srl;
    private RecyclerView rv;

    public MultiPageLoadPresent(Activity activity, View include_refresh_list) {
        this.activity = activity;
        this.srl = (SwipeRefreshLayout) include_refresh_list;
        initView();
    }

    private void initView() {
        rv = (RecyclerView) srl.findViewById(R.id.rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                activity, StaggeredGridLayoutManager.VERTICAL, false);
        rv.setLayoutManager(layoutManager);
        rv.addItemDecoration(new DividerItemDecoration(activity));
    }

    private ArrayList datas;
    private PageIndex pageIndex;
    private MultiPageRequest request;
    private Subscriber subscriber;

    public void load(RecyclerView.Adapter adapter, ArrayList datas,
                     final PageIndex pageIndex, MultiPageRequest request, Subscriber subscriber) {
        this.datas = datas;
        this.pageIndex = pageIndex;
        this.request = request;
        this.subscriber = subscriber;

        setRefreshing(true);
        loadData(this.pageIndex.toStartPage());

        loadMoreAdapter = new LoadMoreAdapter(rv, adapter,
                new LoadMoreAdapter.OnLoadMoreListener() {
                    @Override
                    public void onLoadMore() {
                        // 列表拉到底部时,加载下一页
                        loadData(pageIndex.toNextPage());
                    }
                });
        rv.setAdapter(loadMoreAdapter);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 下拉刷新时,重新加载起始页
                loadData(pageIndex.toStartPage());
            }
        });
    }

    public void setRefreshing(final boolean refreshing) {
        srl.post(new Runnable() {
            @Override
            public void run() {
                srl.setRefreshing(refreshing);
            }
        });
    }

    /**
     * 加载列表
     *
     * @param page 页数
     */
    private void loadData(final int page) {
        Observable observable = this.request.request(page);
        ObservableDecorator.decorate(activity, observable).subscribe(
                new Subscriber<ListResponse>() {
                    @Override
                    public void onNext(ListResponse response) {
                        subscriber.onNext(response);
                        setRefreshing(false);

                        // 加载成功后更新数据
                        pageIndex.setResponse(loadMoreAdapter, datas, response.getResults());
                    }

                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onNext(throwable);
                        setRefreshing(false);
                    }
                });
    }

}
