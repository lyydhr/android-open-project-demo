package com.toaker.circularreveal.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.toaker.circularreveal.sample.widget.FloatingActionButton;
import com.toaker.circularreveal.sample.widget.ViewUtils;
import com.toaker.common.tlog.TLog;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import io.codetail.widget.RevealFrameLayout;

public class MainActivity extends ActionBarActivity{

    public static final boolean DEBUG = true;

    private static int VERSION = 1;

    private static final String LOG_TAG = "circularreveal" + ++VERSION;

    private static final int[] VIEW_ICONS = new int[]{R.drawable.icn_1,R.drawable.icn_2,
                                                        R.drawable.icn_3,R.drawable.icn_4,
                                                        R.drawable.icn_5,R.drawable.icn_6,
                                                        R.drawable.icn_7,};
    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.floatingActionButton)
    FloatingActionButton mFloatingButton;

    @InjectView(R.id.cardsGroup)
    RecyclerView mCardsGroup;

    LinearLayoutManager mLayoutManager;
    RecycleAdapter mCardsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        ButterKnife.setDebug(true);

        setSupportActionBar(mToolbar);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mCardsAdapter = new RecycleAdapter();
        mCardsAdapter.setHasStableIds(true);

        mCardsGroup.setHasFixedSize(true);
        mCardsGroup.setItemViewCacheSize(3);
        mCardsGroup.setClipToPadding(false);
        mCardsGroup.setAdapter(mCardsAdapter);
        mCardsGroup.setLayoutManager(mLayoutManager);

        mToolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeGlobalListeners(mToolbar, this);

                final int outOfScreenY = ((ViewGroup.MarginLayoutParams) mFloatingButton.getLayoutParams())
                        .bottomMargin + mFloatingButton.getHeight();

                ViewAnimationUtils.liftingFromBottom(mFloatingButton, 0, outOfScreenY, 500, 0);

                if(DEBUG){
                    TLog.i(LOG_TAG,"onGlobalLayout");
                }
            }
        });

        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final View myView = ((RevealFrameLayout) mCardsGroup.getChildAt(0)).getChildAt(0);

                // get the center for the clipping circle
                int cx = (myView.getLeft() + myView.getRight()) / 2;
                int cy = (myView.getTop() + myView.getBottom()) / 2;
//                int cx = myView.getRight();
//                int cy = myView.getBottom();

                // get the final radius for the clipping circle
                float finalRadius = hypo(myView.getWidth(), myView.getHeight());

                SupportAnimator animator =
                        ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
                animator.addListener(new SupportAnimator.AnimatorListener() {
                    @Override
                    public void onAnimationStart() {
                        if(DEBUG){
                            TLog.i(LOG_TAG,"onAnimationStart");
                        }
                    }

                    @Override
                    public void onAnimationEnd() {
                        Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_LONG)
                                .show();
                        if(DEBUG){
                            TLog.i(LOG_TAG,"onAnimationEnd");
                        }
                    }

                    @Override
                    public void onAnimationCancel() {
                        if(DEBUG){
                            TLog.i(LOG_TAG,"onAnimationCancel");
                        }
                    }

                    @Override
                    public void onAnimationRepeat() {
                        if(DEBUG){
                            TLog.i(LOG_TAG,"onAnimationRepeat");
                        }
                    }
                });
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(1500);
                animator.start();
            }
        });

    }

    static float hypo(int a, int b){
        return (float) Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCardsGroup.setOnScrollListener(new HideExtraOnScroll(mToolbar));
        if(DEBUG){
            TLog.i(LOG_TAG,"Set OnScrollListener ");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Prevent memory leaks, fuck yeah!
        mCardsGroup.setOnScrollListener(null);
    }

    public static class RecycleAdapter  extends RecyclerView.Adapter<CardHolder>{

        @Override
        public CardHolder onCreateViewHolder(ViewGroup group, int i) {
            LayoutInflater factory = LayoutInflater.from(group.getContext());
            return new CardHolder(factory.inflate(R.layout.card_item, group, false));
        }

        @Override
        public void onBindViewHolder(CardHolder cardHolder, int i) {
            cardHolder.mIconView.setBackgroundResource(VIEW_ICONS[i]);
        }

        @Override
        public int getItemCount() {
            return VIEW_ICONS.length;
        }
    }

    public static class CardHolder extends RecyclerView.ViewHolder{

        @InjectView(R.id.card)
        CardView mCard;

        @InjectView(R.id.icon)
        ImageView mIconView;

        RevealFrameLayout mReveal;

        public CardHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(CardHolder.this, itemView);

            mReveal = (RevealFrameLayout) mCard.getParent();
        }
    }


    public static class HideExtraOnScrollHelper{
        public final static int UNKNOWN = -1;
        public final static int TOP = 0;
        public final static int BOTTOM = 1;

        int mDraggedAmount;
        int mOldDirection;
        int mDragDirection;

        final int mMinFlingDistance;

        public HideExtraOnScrollHelper(int minFlingDistance) {
            mOldDirection  =
                    mDragDirection =
                            mDraggedAmount = UNKNOWN;

            mMinFlingDistance = minFlingDistance;
        }

        /**
         * Checks need to hide extra objects on scroll or not
         *
         * @param dy scrolled distance y
         * @return true if need to hide extra objects on screen
         */
        public boolean isObjectsShouldBeOutside(int dy){
            boolean needHide = false;
            mDragDirection = dy > 0 ? BOTTOM : TOP;

            if(mDragDirection != mOldDirection){
                mDraggedAmount = 0;
            }

            mDraggedAmount += dy;
            boolean shouldBeOutside = false;

            if(mDragDirection == TOP && Math.abs(mDraggedAmount) > mMinFlingDistance){
                shouldBeOutside = false;
            }else if(mDragDirection == BOTTOM && mDraggedAmount > mMinFlingDistance){
                shouldBeOutside = true;
            }

            if(mOldDirection != mDragDirection){
                mOldDirection = mDragDirection;
            }

            if(DEBUG){
                TLog.i(LOG_TAG,"isObjectsShouldBeOutside dy:%s ",dy);
            }

            return shouldBeOutside;
        }
    }


    public static class HideExtraOnScroll extends RecyclerView.OnScrollListener{

        final static Interpolator ACCELERATE = new AccelerateInterpolator();
        final static Interpolator DECELERATE = new DecelerateInterpolator();

        WeakReference<View> mTarget;
        HideExtraOnScrollHelper mScrollHelper;

        boolean isExtraObjectsOutside;

        public HideExtraOnScroll(View target) {
            int minimumFlingVelocity = ViewConfiguration.get(target.getContext())
                    .getScaledMinimumFlingVelocity();

            mScrollHelper = new HideExtraOnScrollHelper(minimumFlingVelocity);
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            final View target = mTarget.get();

            if(target == null) {
                return;
            }

            boolean isObjectsShouldBeOutside = mScrollHelper.isObjectsShouldBeOutside(dy);

            if(!isVisible(target) && !isObjectsShouldBeOutside){
                show(target);
                isExtraObjectsOutside = false;
            }else if(isVisible(target) && isObjectsShouldBeOutside){
                hide(target, -target.getHeight());
                isExtraObjectsOutside = true;
            }
        }

        public boolean isVisible(View target){
            return !isExtraObjectsOutside;
        }

        public void hide(final View target, float distance){
            ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationY",
                    ViewHelper.getTranslationY(target), distance);
            animator.setInterpolator(DECELERATE);
            animator.start();
        }

        public void show(final View target){
            ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationY",
                    ViewHelper.getTranslationY(target), 0f);
            animator.setInterpolator(ACCELERATE);
            animator.start();
        }

    }

}