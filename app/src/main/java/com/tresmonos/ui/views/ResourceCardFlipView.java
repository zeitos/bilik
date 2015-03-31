package com.tresmonos.ui.views;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.vsc.google.api.services.samples.calendar.android.bilik.R;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * http://fortawesome.github.io/Font-Awesome/cheatsheet/
 */
public class ResourceCardFlipView extends FrameLayout {

    //TODO: this should be implemented outside the {@link ResourceCardFlipView}, but we should start
    // managing listeners... and I prefer not to do it right now...
    private final static Timer dismissTimer;
    private static final Duration FLIP_EXPIRATION_DURATION = Duration.standardSeconds(7);
    private final static List<ResourceCardFlipView> flippedViews = new CopyOnWriteArrayList();
    private final Context context;
    private DateTime expirationDate;
    private boolean flippingEnabled;
	private volatile boolean mEnableAnimation = true;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final View frontCardView = getFromCardView();
        final View backCardView = getBackCardView();
        final TextView backIconView = (TextView)backCardView.findViewById(R.id.back_icon);

        Typeface font = Typeface.createFromAsset(context.getAssets(), "fontawesome-webfont.ttf");
        backIconView.setTypeface(font);
        frontCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flippingEnabled) {
                    flipIn(frontCardView, backCardView);
                }
            }
        });

        backCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipOut(backCardView, frontCardView);
            }
        });
    }

	private View getFromCardView() {
		return findViewById(R.id.resource_card_front);
	}

	public void flipIn() {
        flipIn(getFromCardView(), getBackCardView());
    }

    public void flipOut() {
        flipOut(getBackCardView(), getFromCardView());
    }

	private boolean isFlipIn() {
		return getBackCardView().getVisibility() == View.VISIBLE;
	}

	private View getBackCardView() {
		return findViewById(R.id.resource_card_back);
	}

	private void flipOut(final View backCardView, final View frontCardView) {
		if (mEnableAnimation) {
			post(new Runnable() {
				@Override
				public void run() {
					AnimatorSet flipLeftOutAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.card_flip_left_out);
					flipLeftOutAnimation.setTarget(backCardView);
					flipLeftOutAnimation.start();
					frontCardView.setVisibility(View.VISIBLE);
					AnimatorSet flipLeftInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.card_flip_left_in);
					flipLeftInAnimation.setTarget(frontCardView);
					flipLeftInAnimation.addListener(new Animator.AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
							mEnableAnimation = false;
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							backCardView.setVisibility(View.INVISIBLE);
							mEnableAnimation = true;
						}

						@Override
						public void onAnimationCancel(Animator animation) {

						}

						@Override
						public void onAnimationRepeat(Animator animation) {

						}
					});
					flipLeftInAnimation.start();
				}
			});
		}
    }

    private void flipIn(final View frontCardView, final View backCardView) {
	    if (mEnableAnimation) {
		    post(new Runnable() {
			    @Override
			    public void run() {
				    AnimatorSet flipRightOutAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.card_flip_right_out);
				    flipRightOutAnimation.setTarget(frontCardView);
				    flipRightOutAnimation.start();
				    backCardView.setVisibility(View.VISIBLE);
				    AnimatorSet flipRightInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.card_flip_right_in);
				    flipRightInAnimation.setTarget(backCardView);
				    flipRightInAnimation.addListener(new Animator.AnimatorListener() {

					    @Override
					    public void onAnimationStart(Animator animation) {
						    mEnableAnimation = false;
					    }

					    @Override
					    public void onAnimationEnd(Animator animation) {
						    frontCardView.setVisibility(View.INVISIBLE);
						    expirationDate = DateTime.now().plus(FLIP_EXPIRATION_DURATION);
						    flippedViews.add(ResourceCardFlipView.this);
						    mEnableAnimation = true;
					    }

					    @Override
					    public void onAnimationCancel(Animator animation) {

					    }

					    @Override
					    public void onAnimationRepeat(Animator animation) {

					    }
				    });
				    flipRightInAnimation.start();

			    }
		    });
	    }
    }


    public ResourceCardFlipView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ViewHolder createViewHolder() {
        return new ViewHolder((TextView) findViewById(R.id.resource_card_title_text_view),
                (TextView) findViewById(R.id.resource_card_title_back_text_view),
                findViewById(R.id.status_layout),
                (DataUnitView) findViewById(R.id.remaining_hours),
                (DataUnitView) findViewById(R.id.remaining_minutes),
                (Button) findViewById(R.id.button_view),
                findViewById(R.id.flip_card_icon),
		        findViewById(R.id.resource_sync_in_progress_view));
    }

    public void setFlippingEnabled(boolean flippingEnabled) {
        this.flippingEnabled = flippingEnabled;
    }

	public void ensureFront() {
		if (getFromCardView().getVisibility() != VISIBLE) {
			flipOut();
		}
	}

	public static class ViewHolder {
        public final TextView frontTitleView;
        public final TextView backTitleView;
        public final View statusLayoutView;
        public final DataUnitView remainingHoursView;
        public final DataUnitView remainingMinutesView;
        public final Button buttonView;
        public final View flipCardIconView;
		public final View resourceInProgresView;

		private ViewHolder(TextView frontTitleView, TextView backTitleView, View statusLayoutView, DataUnitView remainingHoursView, DataUnitView remainingMinutesView, Button buttonView, View flipCardIconView, View resourceInProgresView) {
            this.frontTitleView = frontTitleView;
            this.backTitleView = backTitleView;
            this.statusLayoutView = statusLayoutView;
            this.remainingHoursView = remainingHoursView;
            this.remainingMinutesView = remainingMinutesView;
            this.buttonView = buttonView;
            this.flipCardIconView = flipCardIconView;
			this.resourceInProgresView = resourceInProgresView;
        }

    }

    static {
        dismissTimer = new Timer();
        dismissTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DateTime now = DateTime.now();
                for (ResourceCardFlipView view : flippedViews) {
                    if (now.isAfter(view.expirationDate) && view.isFlipIn()) {
                        view.expirationDate = null;
                        flippedViews.remove(view);
                        view.flipOut();
                    }
                }
            }

        }, 0, 1500);
    }
}
