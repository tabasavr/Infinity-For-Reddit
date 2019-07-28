package ml.docilealligator.infinityforreddit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;
import javax.inject.Named;

import SubredditDatabase.SubredditDao;
import SubredditDatabase.SubredditData;
import SubredditDatabase.SubredditRoomDatabase;
import SubredditDatabase.SubredditViewModel;
import SubscribedSubredditDatabase.SubscribedSubredditDao;
import SubscribedSubredditDatabase.SubscribedSubredditRoomDatabase;
import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import pl.droidsonroids.gif.GifImageView;
import retrofit2.Retrofit;

public class ViewSubredditDetailActivity extends AppCompatActivity implements SortTypeBottomSheetFragment.SortTypeSelectionCallback,
        PostTypeBottomSheetFragment.PostTypeSelectionCallback {

    public static final String EXTRA_SUBREDDIT_NAME_KEY = "ESN";

    private static final String FRAGMENT_OUT_STATE_KEY = "FOSK";
    private static final String IS_IN_LAZY_MODE_STATE = "IILMS";

    @BindView(R.id.coordinator_layout_view_subreddit_detail_activity) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.appbar_layout_view_subreddit_detail) AppBarLayout appBarLayout;
    @BindView(R.id.collapsing_toolbar_layout_view_subreddit_detail_activity) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.banner_image_view_view_subreddit_detail_activity) GifImageView bannerImageView;
    @BindView(R.id.icon_gif_image_view_view_subreddit_detail_activity) GifImageView iconGifImageView;
    @BindView(R.id.subscribe_subreddit_chip_view_subreddit_detail_activity) Chip subscribeSubredditChip;
    @BindView(R.id.subreddit_name_text_view_view_subreddit_detail_activity) TextView subredditNameTextView;
    @BindView(R.id.subscriber_count_text_view_view_subreddit_detail_activity) TextView nSubscribersTextView;
    @BindView(R.id.online_subscriber_count_text_view_view_subreddit_detail_activity) TextView nOnlineSubscribersTextView;
    @BindView(R.id.description_text_view_view_subreddit_detail_activity) TextView descriptionTextView;
    @BindView(R.id.fab_view_subreddit_detail_activity) FloatingActionButton fab;

    private String subredditName;
    private boolean subscriptionReady = false;
    private boolean isInLazyMode = false;

    private RequestManager glide;
    private Fragment mFragment;
    private Menu mMenu;
    private AppBarLayout.LayoutParams params;
    private PostTypeBottomSheetFragment postTypeBottomSheetFragment;
    private SortTypeBottomSheetFragment sortTypeBottomSheetFragment;

    private SubscribedSubredditDao subscribedSubredditDao;
    private SubredditViewModel mSubredditViewModel;

    @Inject
    @Named("no_oauth")
    Retrofit mRetrofit;

    @Inject
    @Named("oauth")
    Retrofit mOauthRetrofit;

    @Inject
    @Named("auth_info")
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_subreddit_detail);

        ButterKnife.bind(this);

        ((Infinity) getApplication()).getmAppComponent().inject(this);

        postTypeBottomSheetFragment = new PostTypeBottomSheetFragment();
        sortTypeBottomSheetFragment = new SortTypeBottomSheetFragment();

        params = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();

        //Get status bar height
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        subredditName = getIntent().getExtras().getString(EXTRA_SUBREDDIT_NAME_KEY);
        String title = "r/" + subredditName;
        subredditNameTextView.setText(title);

        toolbar.setTitle(title);
        setSupportActionBar(toolbar);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.topMargin = statusBarHeight;

        subscribedSubredditDao = SubscribedSubredditRoomDatabase.getDatabase(this).subscribedSubredditDao();
        glide = Glide.with(this);

        mSubredditViewModel = ViewModelProviders.of(this, new SubredditViewModel.Factory(getApplication(), subredditName))
                .get(SubredditViewModel.class);
        mSubredditViewModel.getSubredditLiveData().observe(this, subredditData -> {
            if(subredditData != null) {
                if(subredditData.getBannerUrl().equals("")) {
                    iconGifImageView.setOnClickListener(view -> {
                        //Do nothing as it has no image
                    });
                } else {
                    glide.load(subredditData.getBannerUrl()).into(bannerImageView);
                    bannerImageView.setOnClickListener(view -> {
                        Intent intent = new Intent(ViewSubredditDetailActivity.this, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.TITLE_KEY, title);
                        intent.putExtra(ViewImageActivity.IMAGE_URL_KEY, subredditData.getBannerUrl());
                        intent.putExtra(ViewImageActivity.FILE_NAME_KEY, subredditName + "-banner");
                        startActivity(intent);
                    });
                }

                if(subredditData.getIconUrl().equals("")) {
                    glide.load(getDrawable(R.drawable.subreddit_default_icon))
                            .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(216, 0)))
                            .into(iconGifImageView);
                    iconGifImageView.setOnClickListener(view -> {
                        //Do nothing as it is a default icon
                    });
                } else {
                    glide.load(subredditData.getIconUrl())
                            .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(216, 0)))
                            .error(glide.load(R.drawable.subreddit_default_icon)
                                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(216, 0))))
                            .into(iconGifImageView);
                    iconGifImageView.setOnClickListener(view -> {
                        Intent intent = new Intent(ViewSubredditDetailActivity.this, ViewImageActivity.class);
                        intent.putExtra(ViewImageActivity.TITLE_KEY, title);
                        intent.putExtra(ViewImageActivity.IMAGE_URL_KEY, subredditData.getIconUrl());
                        intent.putExtra(ViewImageActivity.FILE_NAME_KEY, subredditName + "-icon");
                        startActivity(intent);
                    });
                }

                String subredditFullName = "r/" + subredditData.getName();
                if(!title.equals(subredditFullName)) {
                    getSupportActionBar().setTitle(subredditFullName);
                }
                subredditNameTextView.setText(subredditFullName);
                String nSubscribers = getString(R.string.subscribers_number_detail, subredditData.getNSubscribers());
                nSubscribersTextView.setText(nSubscribers);
                if(subredditData.getDescription().equals("")) {
                    descriptionTextView.setVisibility(View.GONE);
                } else {
                    descriptionTextView.setVisibility(View.VISIBLE);
                    descriptionTextView.setText(subredditData.getDescription());
                }
            }
        });

        subscribeSubredditChip.setOnClickListener(view -> {
            if(subscriptionReady) {
                subscriptionReady = false;
                if(subscribeSubredditChip.getText().equals(getResources().getString(R.string.subscribe))) {
                    SubredditSubscription.subscribeToSubreddit(mOauthRetrofit, mRetrofit, sharedPreferences,
                            subredditName, subscribedSubredditDao, new SubredditSubscription.SubredditSubscriptionListener() {
                                @Override
                                public void onSubredditSubscriptionSuccess() {
                                    subscribeSubredditChip.setText(R.string.unsubscribe);
                                    subscribeSubredditChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorAccent));
                                    makeSnackbar(R.string.subscribed);
                                    subscriptionReady = true;
                                }

                                @Override
                                public void onSubredditSubscriptionFail() {
                                    makeSnackbar(R.string.subscribe_failed);
                                    subscriptionReady = true;
                                }
                            });
                } else {
                    SubredditSubscription.unsubscribeToSubreddit(mOauthRetrofit, sharedPreferences,
                            subredditName, subscribedSubredditDao, new SubredditSubscription.SubredditSubscriptionListener() {
                                @Override
                                public void onSubredditSubscriptionSuccess() {
                                    subscribeSubredditChip.setText(R.string.subscribe);
                                    subscribeSubredditChip.setChipBackgroundColor(getResources().getColorStateList(R.color.backgroundColorPrimaryDark));
                                    makeSnackbar(R.string.unsubscribed);
                                    subscriptionReady = true;
                                }

                                @Override
                                public void onSubredditSubscriptionFail() {
                                    makeSnackbar(R.string.unsubscribe_failed);
                                    subscriptionReady = true;
                                }
                            });
                }
            }
        });

        new CheckIsSubscribedToSubredditAsyncTask(subscribedSubredditDao, subredditName,
                new CheckIsSubscribedToSubredditAsyncTask.CheckIsSubscribedToSubredditListener() {
            @Override
            public void isSubscribed() {
                subscribeSubredditChip.setText(R.string.unsubscribe);
                subscribeSubredditChip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorAccent));
                subscriptionReady = true;
            }

            @Override
            public void isNotSubscribed() {
                subscribeSubredditChip.setText(R.string.subscribe);
                subscribeSubredditChip.setChipBackgroundColor(getResources().getColorStateList(R.color.backgroundColorPrimaryDark));
                subscriptionReady = true;
            }
        }).execute();

        FetchSubredditData.fetchSubredditData(mRetrofit, subredditName, new FetchSubredditData.FetchSubredditDataListener() {
            @Override
            public void onFetchSubredditDataSuccess(SubredditData subredditData, int nCurrentOnlineSubscribers) {
                new InsertSubredditDataAsyncTask(SubredditRoomDatabase.getDatabase(ViewSubredditDetailActivity.this), subredditData)
                        .execute();
                String nOnlineSubscribers = getString(R.string.online_subscribers_number_detail, nCurrentOnlineSubscribers);
                nOnlineSubscribersTextView.setText(nOnlineSubscribers);
            }

            @Override
            public void onFetchSubredditDataFail() {

            }
        });

        if(savedInstanceState == null) {
            mFragment = new PostFragment();
            Bundle bundle = new Bundle();
            bundle.putString(PostFragment.EXTRA_SUBREDDIT_NAME, subredditName);
            bundle.putInt(PostFragment.EXTRA_POST_TYPE, PostDataSource.TYPE_SUBREDDIT);
            bundle.putString(PostFragment.EXTRA_SORT_TYPE, PostDataSource.SORT_TYPE_BEST);
            mFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_view_subreddit_detail_activity, mFragment).commit();
        } else {
            mFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_OUT_STATE_KEY);
            if(mFragment == null) {
                mFragment = new PostFragment();
                Bundle bundle = new Bundle();
                bundle.putString(PostFragment.EXTRA_SUBREDDIT_NAME, subredditName);
                bundle.putInt(PostFragment.EXTRA_POST_TYPE, PostDataSource.TYPE_SUBREDDIT);
                bundle.putString(PostFragment.EXTRA_SORT_TYPE, PostDataSource.SORT_TYPE_BEST);
                mFragment.setArguments(bundle);
            }
            isInLazyMode = savedInstanceState.getBoolean(IS_IN_LAZY_MODE_STATE);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_view_subreddit_detail_activity, mFragment).commit();
        }

        fab.setOnClickListener(view -> postTypeBottomSheetFragment.show(getSupportFragmentManager(), postTypeBottomSheetFragment.getTag()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_subreddit_detail_activity, menu);
        mMenu = menu;
        MenuItem lazyModeItem = mMenu.findItem(R.id.action_lazy_mode_view_subreddit_detail_activity);
        if(isInLazyMode) {
            lazyModeItem.setTitle(R.string.action_stop_lazy_mode);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
            collapsingToolbarLayout.setLayoutParams(params);
        } else {
            lazyModeItem.setTitle(R.string.action_start_lazy_mode);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS |
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED);
            collapsingToolbarLayout.setLayoutParams(params);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_sort_view_subreddit_detail_activity:
                sortTypeBottomSheetFragment.show(getSupportFragmentManager(), sortTypeBottomSheetFragment.getTag());
                return true;
            case R.id.action_search_view_subreddit_detail_activity:
                Intent intent = new Intent(this, SearchActivity.class);
                intent.putExtra(SearchActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                intent.putExtra(SearchActivity.EXTRA_SUBREDDIT_IS_USER, false);
                startActivity(intent);
                break;
            case R.id.action_refresh_view_subreddit_detail_activity:
                if(mFragment instanceof FragmentCommunicator) {
                    ((FragmentCommunicator) mFragment).refresh();
                }
                break;
            case R.id.action_lazy_mode_view_subreddit_detail_activity:
                MenuItem lazyModeItem = mMenu.findItem(R.id.action_lazy_mode_view_subreddit_detail_activity);
                if(isInLazyMode) {
                    isInLazyMode = false;
                    ((FragmentCommunicator) mFragment).stopLazyMode();
                    lazyModeItem.setTitle(R.string.action_start_lazy_mode);
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS |
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED);
                    collapsingToolbarLayout.setLayoutParams(params);
                } else {
                    isInLazyMode = true;
                    ((FragmentCommunicator) mFragment).startLazyMode();
                    lazyModeItem.setTitle(R.string.action_stop_lazy_mode);
                    appBarLayout.setExpanded(false);
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
                    collapsingToolbarLayout.setLayoutParams(params);
                }
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_IN_LAZY_MODE_STATE, isInLazyMode);
        getSupportFragmentManager().putFragment(outState, FRAGMENT_OUT_STATE_KEY, mFragment);
    }

    private void makeSnackbar(int resId) {
        Snackbar.make(coordinatorLayout, resId, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void sortTypeSelected(String sortType) {
        ((PostFragment) mFragment).changeSortType(sortType);
    }

    @Override
    public void postTypeSelected(int postType) {
        Intent intent;
        switch (postType) {
            case PostTypeBottomSheetFragment.TYPE_TEXT:
                intent = new Intent(this, PostTextActivity.class);
                intent.putExtra(PostTextActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                startActivity(intent);
                break;
            case PostTypeBottomSheetFragment.TYPE_LINK:
                intent = new Intent(this, PostLinkActivity.class);
                intent.putExtra(PostTextActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                startActivity(intent);
                break;
            case PostTypeBottomSheetFragment.TYPE_IMAGE:
                intent = new Intent(this, PostImageActivity.class);
                intent.putExtra(PostTextActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                startActivity(intent);
                break;
            case PostTypeBottomSheetFragment.TYPE_VIDEO:
                intent = new Intent(this, PostVideoActivity.class);
                intent.putExtra(PostTextActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                startActivity(intent);
        }
    }

    private static class InsertSubredditDataAsyncTask extends AsyncTask<Void, Void, Void> {

        private SubredditDao mSubredditDao;
        private SubredditData subredditData;

        InsertSubredditDataAsyncTask(SubredditRoomDatabase subredditDb, SubredditData subredditData) {
            mSubredditDao = subredditDb.subredditDao();
            this.subredditData = subredditData;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mSubredditDao.insert(subredditData);
            return null;
        }
    }
}
