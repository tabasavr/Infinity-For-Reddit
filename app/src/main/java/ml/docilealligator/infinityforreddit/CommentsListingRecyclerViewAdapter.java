package ml.docilealligator.infinityforreddit;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import CustomView.CustomMarkwonView;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Retrofit;

class CommentsListingRecyclerViewAdapter extends PagedListAdapter<CommentData, RecyclerView.ViewHolder> {
    private Context mContext;
    private Retrofit mOauthRetrofit;
    private String mAccessToken;
    private int mTextColorPrimaryDark;
    private int mColorAccent;

    private static final int VIEW_TYPE_DATA = 0;
    private static final int VIEW_TYPE_ERROR = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private NetworkState networkState;
    private RetryLoadingMoreCallback mRetryLoadingMoreCallback;

    interface RetryLoadingMoreCallback {
        void retryLoadingMore();
    }

    protected CommentsListingRecyclerViewAdapter(Context context, Retrofit oauthRetrofit, String accessToken,
                                                 RetryLoadingMoreCallback retryLoadingMoreCallback) {
        super(DIFF_CALLBACK);
        mContext = context;
        mOauthRetrofit = oauthRetrofit;
        mAccessToken = accessToken;
        mRetryLoadingMoreCallback = retryLoadingMoreCallback;
        mTextColorPrimaryDark = mContext.getResources().getColor(R.color.textColorPrimaryDark);
        mColorAccent = mContext.getResources().getColor(R.color.colorAccent);
    }

    private static final DiffUtil.ItemCallback<CommentData> DIFF_CALLBACK = new DiffUtil.ItemCallback<CommentData>() {
        @Override
        public boolean areItemsTheSame(@NonNull CommentData CommentData, @NonNull CommentData t1) {
            return CommentData.getId().equals(t1.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CommentData CommentData, @NonNull CommentData t1) {
            return CommentData.getCommentContent().equals(t1.getCommentContent());
        }
    };

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_DATA) {
            CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new DataViewHolder(cardView);
        } else if(viewType == VIEW_TYPE_ERROR) {
            RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer_error, parent, false);
            return new ErrorViewHolder(relativeLayout);
        } else {
            RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer_loading, parent, false);
            return new LoadingViewHolder(relativeLayout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof DataViewHolder) {
            CommentData comment = getItem(holder.getAdapterPosition());

            if(comment.getAuthor().equals(comment.getSubredditName().substring(2))) {
                ((DataViewHolder) holder).authorTextView.setText("u/" + comment.getAuthor());
                ((DataViewHolder) holder).authorTextView.setTextColor(mTextColorPrimaryDark);
                ((DataViewHolder) holder).authorTextView.setOnClickListener(view -> {
                    Intent intent = new Intent(mContext, ViewUserDetailActivity.class);
                    intent.putExtra(ViewUserDetailActivity.EXTRA_USER_NAME_KEY, comment.getAuthor());
                    mContext.startActivity(intent);
                });
            } else {
                ((DataViewHolder) holder).authorTextView.setText("r/" + comment.getSubredditName());
                ((DataViewHolder) holder).authorTextView.setTextColor(mColorAccent);
                ((DataViewHolder) holder).authorTextView.setOnClickListener(view -> {
                    Intent intent = new Intent(mContext, ViewSubredditDetailActivity.class);
                    intent.putExtra(ViewSubredditDetailActivity.EXTRA_SUBREDDIT_NAME_KEY, comment.getSubredditName());
                    mContext.startActivity(intent);
                });
            }

            ((DataViewHolder) holder).commentTimeTextView.setText(comment.getCommentTime());

            ((DataViewHolder) holder).commentMarkdownView.setMarkdown(comment.getCommentContent(), mContext);
            ((DataViewHolder) holder).scoreTextView.setText(Integer.toString(comment.getScore()));

            switch (comment.getVoteType()) {
                case 1:
                    ((DataViewHolder) holder).upvoteButton
                            .setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                    break;
                case 2:
                    ((DataViewHolder) holder).downvoteButton
                            .setColorFilter(ContextCompat.getColor(mContext, R.color.minusButtonColor), android.graphics.PorterDuff.Mode.SRC_IN);
                    break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        // Reached at the end
        if (hasExtraRow() && position == getItemCount() - 1) {
            if (networkState.getStatus() == NetworkState.Status.LOADING) {
                return VIEW_TYPE_LOADING;
            } else {
                return VIEW_TYPE_ERROR;
            }
        } else {
            return VIEW_TYPE_DATA;
        }
    }

    @Override
    public int getItemCount() {
        if(hasExtraRow()) {
            return super.getItemCount() + 1;
        }
        return super.getItemCount();
    }

    private boolean hasExtraRow() {
        return networkState != null && networkState.getStatus() != NetworkState.Status.SUCCESS;
    }

    void setNetworkState(NetworkState newNetworkState) {
        NetworkState previousState = this.networkState;
        boolean previousExtraRow = hasExtraRow();
        this.networkState = newNetworkState;
        boolean newExtraRow = hasExtraRow();
        if (previousExtraRow != newExtraRow) {
            if (previousExtraRow) {
                notifyItemRemoved(super.getItemCount());
            } else {
                notifyItemInserted(super.getItemCount());
            }
        } else if (newExtraRow && !previousState.equals(newNetworkState)) {
            notifyItemChanged(getItemCount() - 1);
        }
    }

    class DataViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_view_item_comment) MaterialCardView cardView;
        @BindView(R.id.vertical_block_item_post_comment) View verticalBlock;
        @BindView(R.id.author_text_view_item_post_comment) TextView authorTextView;
        @BindView(R.id.comment_time_text_view_item_post_comment) TextView commentTimeTextView;
        @BindView(R.id.comment_markdown_view_item_post_comment) CustomMarkwonView commentMarkdownView;
        @BindView(R.id.plus_button_item_post_comment) ImageView upvoteButton;
        @BindView(R.id.score_text_view_item_post_comment) TextView scoreTextView;
        @BindView(R.id.minus_button_item_post_comment) ImageView downvoteButton;
        @BindView(R.id.share_button_item_post_comment) ImageView shareButton;
        @BindView(R.id.reply_button_item_post_comment) ImageView replyButton;

        DataViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            cardView.setOnClickListener(view -> {
                Intent intent = new Intent(mContext, ViewPostDetailActivity.class);
                intent.putExtra(ViewPostDetailActivity.EXTRA_POST_ID, getItem(getAdapterPosition()).getLinkId());
                mContext.startActivity(intent);
            });

            verticalBlock.setVisibility(View.GONE);

            commentMarkdownView.setOnClickListener(view -> cardView.callOnClick());

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) shareButton.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.setMarginEnd(0);
            shareButton.setLayoutParams(lp);

            shareButton.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                String extraText = getItem(getAdapterPosition()).getPermalink();
                intent.putExtra(Intent.EXTRA_TEXT, extraText);
                mContext.startActivity(Intent.createChooser(intent, "Share"));
            });

            replyButton.setVisibility(View.GONE);

            upvoteButton.setOnClickListener(view -> {
                int previousVoteType = getItem(getAdapterPosition()).getVoteType();
                String newVoteType;

                downvoteButton.clearColorFilter();

                if(previousVoteType != CommentData.VOTE_TYPE_UPVOTE) {
                    //Not upvoted before
                    getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_UPVOTE);
                    newVoteType = RedditUtils.DIR_UPVOTE;
                    upvoteButton.setColorFilter(ContextCompat.getColor(mContext, R.color.backgroundColorPrimaryDark), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    //Upvoted before
                    getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_NO_VOTE);
                    newVoteType = RedditUtils.DIR_UNVOTE;
                    upvoteButton.clearColorFilter();
                }

                scoreTextView.setText(Integer.toString(getItem(getAdapterPosition()).getScore() + getItem(getAdapterPosition()).getVoteType()));

                VoteThing.voteThing(mOauthRetrofit, mAccessToken, new VoteThing.VoteThingListener() {
                    @Override
                    public void onVoteThingSuccess(int position) {
                        if(newVoteType.equals(RedditUtils.DIR_UPVOTE)) {
                            getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_UPVOTE);
                            upvoteButton.setColorFilter(ContextCompat.getColor(mContext, R.color.backgroundColorPrimaryDark), android.graphics.PorterDuff.Mode.SRC_IN);
                        } else {
                            getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_NO_VOTE);
                            upvoteButton.clearColorFilter();
                        }

                        downvoteButton.clearColorFilter();
                        scoreTextView.setText(Integer.toString(getItem(getAdapterPosition()).getScore() + getItem(getAdapterPosition()).getVoteType()));
                    }

                    @Override
                    public void onVoteThingFail(int position) { }
                }, getItem(getAdapterPosition()).getFullName(), newVoteType, getAdapterPosition());
            });

            downvoteButton.setOnClickListener(view -> {
                int previousVoteType = getItem(getAdapterPosition()).getVoteType();
                String newVoteType;

                upvoteButton.clearColorFilter();

                if(previousVoteType != CommentData.VOTE_TYPE_DOWNVOTE) {
                    //Not downvoted before
                    getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_DOWNVOTE);
                    newVoteType = RedditUtils.DIR_DOWNVOTE;
                    downvoteButton.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    //Downvoted before
                    getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_NO_VOTE);
                    newVoteType = RedditUtils.DIR_UNVOTE;
                    downvoteButton.clearColorFilter();
                }

                scoreTextView.setText(Integer.toString(getItem(getAdapterPosition()).getScore() + getItem(getAdapterPosition()).getVoteType()));

                VoteThing.voteThing(mOauthRetrofit, mAccessToken, new VoteThing.VoteThingListener() {
                    @Override
                    public void onVoteThingSuccess(int position1) {
                        if(newVoteType.equals(RedditUtils.DIR_DOWNVOTE)) {
                            getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_DOWNVOTE);
                            downvoteButton.setColorFilter(ContextCompat.getColor(mContext, R.color.colorAccent), android.graphics.PorterDuff.Mode.SRC_IN);
                        } else {
                            getItem(getAdapterPosition()).setVoteType(CommentData.VOTE_TYPE_NO_VOTE);
                            downvoteButton.clearColorFilter();
                        }

                        upvoteButton.clearColorFilter();
                        scoreTextView.setText(Integer.toString(getItem(getAdapterPosition()).getScore() + getItem(getAdapterPosition()).getVoteType()));
                    }

                    @Override
                    public void onVoteThingFail(int position1) { }
                }, getItem(getAdapterPosition()).getFullName(), newVoteType, getAdapterPosition());
            });
        }
    }

    class ErrorViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.error_text_view_item_footer_error) TextView errorTextView;
        @BindView(R.id.retry_button_item_footer_error) Button retryButton;

        ErrorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            errorTextView.setText(R.string.load_comments_failed);
            retryButton.setOnClickListener(view -> mRetryLoadingMoreCallback.retryLoadingMore());
        }
    }

    class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}