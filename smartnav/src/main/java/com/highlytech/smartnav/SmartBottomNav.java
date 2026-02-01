package com.highlytech.smartnav;

import static android.widget.LinearLayout.HORIZONTAL;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SmartBottomNav extends RelativeLayout {

    private Context context;
    private final String TAG = "SmartBottomNavBar";

    // Configuration from XML attributes
    private int menuResId;
    private float navItemWidth, navItemHeight;
    private int navIconSize;
    private int navBackgroundColor, navTextColor, navItemSelectColor;
    private int[] navItemColors = {
            Color.parseColor("#2196F3"), Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"), Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0")
    };

    // View and State variables
    private List<NavMenuItem> menuList = new ArrayList<>();
    private LinearLayout navContainer;
    private int selectedPosition = 100;
    private boolean isInitialized = false;
    private ViewPager2 viewPager2;
    private int pagerContainerInt;

    private FragmentManager fragmentManager;
    private ArrayList<Fragment> fragments;

    // --- Back Stack Variables ---
    private boolean isBackNav = false;
    private boolean isBackStack = false;
    private ArrayList<Integer> backStackList;

    // Listeners
    private OnNavItemSelectedListener navItemSelectedListener;

    public SmartBottomNav(Context context) {
        super(context);
        init(context, null);
    }

    public SmartBottomNav(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SmartBottomNav(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        if (attrs != null) {
            @SuppressLint("CustomViewStyleable")
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SmartBottomNavBar);
            try {
                menuResId = typedArray.getResourceId(R.styleable.SmartBottomNavBar_menuResourceItem, -1);
                pagerContainerInt = typedArray.getResourceId(R.styleable.SmartBottomNavBar_viewPager2, -1);
                navItemWidth = typedArray.getDimension(R.styleable.SmartBottomNavBar_navItemWidth, LayoutParams.WRAP_CONTENT);
                navItemHeight = typedArray.getDimension(R.styleable.SmartBottomNavBar_navItemHeight, dpToPx(35));
                navIconSize = (int) typedArray.getDimension(R.styleable.SmartBottomNavBar_navIconSize, dpToPx(27));
                navBackgroundColor = typedArray.getColor(R.styleable.SmartBottomNavBar_navBackgroundColor, Color.parseColor("#0C5368FF"));
                navTextColor = typedArray.getColor(R.styleable.SmartBottomNavBar_navTextColor, Color.WHITE);
                navItemSelectColor = typedArray.getColor(R.styleable.SmartBottomNavBar_navItemSelectColor, Color.parseColor("#00D0C6"));
            } finally {
                typedArray.recycle();
            }
        }

        // Load menu from XML if provided
        if (menuResId != -1) {
            loadMenuFromXml(menuResId);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInitialized) {
            // Set the background after inflation to ensure correct size and drawing
            Drawable background = getBackground();
            if (background == null || (background instanceof ColorDrawable)) {
                setBackground(createBottomNavBg(navBackgroundColor));
            }

            selectedPosition = 100;

            createNavView(menuList);
            isInitialized = true;
        }
    }


    private void loadMenuFromXml(int menuResId) {
        Menu menu = new PopupMenu(context, null).getMenu();
        new MenuInflater(context).inflate(menuResId, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            NavMenuItem menuItem = new NavMenuItem(item.getItemId(), item.getIcon(), Objects.requireNonNull(item.getTitle(),"def").toString());
            menuList.add(menuItem);
        }

        createNavView(menuList);
    }

    private void createNavView(List<NavMenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return;
        removeAllViews();

        navContainer = new LinearLayout(context);
        navContainer.setOrientation(HORIZONTAL);
        navContainer.setWeightSum(menuList.size());
        navContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (int x = 0; x < menuList.size(); x++) {
            NavMenuItem menuItem = menuList.get(x);
            LinearLayout navItemContainer = new LinearLayout(context);
            navItemContainer.setTag("nav_Item_container_" + x);
            navItemContainer.setGravity(Gravity.CENTER);
            navItemContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams itemConParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
            navItemContainer.setLayoutParams(itemConParams);

            RelativeLayout navItem = new RelativeLayout(context);
            navItem.setId(menuItem.getId());
            navItem.setTag("nav_item_" + x);
            int navItemPadding = dpToPx(5);
            navItem.setPadding(navItemPadding, 0, navItemPadding, 0);
            navItem.setClipToPadding(true);
            navItem.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams navItemParams = new LinearLayout.LayoutParams((int) navItemWidth, (int) navItemHeight);
            navItem.setLayoutParams(navItemParams);

            ImageView navIcon = new ImageView(context);
            navIcon.setId(View.generateViewId());
            navIcon.setImageDrawable(menuItem.getIcon());
            navIcon.setTag("nav_icon_" + x);
            LayoutParams iconParams = new LayoutParams(navIconSize, navIconSize);
            iconParams.addRule(RelativeLayout.CENTER_VERTICAL);
            navIcon.setLayoutParams(iconParams);
            navIcon.setPadding(0, 0, dpToPx(2), 0);
            navIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Set icon color
            navIcon.setColorFilter(getNavItemColor(x));

            TextView navTitle = new TextView(context);
            navTitle.setId(View.generateViewId());
            navTitle.setTag("nav_title_" + x);
            navTitle.setText(menuItem.getTitle());
            navTitle.setMaxLines(1);
            navTitle.setTextColor(navTextColor);
            navTitle.setGravity(Gravity.CENTER_VERTICAL);
            navTitle.setVisibility(View.GONE);
            LayoutParams titleParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            titleParams.addRule(RelativeLayout.RIGHT_OF, navIcon.getId());
            titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
            navTitle.setLayoutParams(titleParams);

            navItem.addView(navIcon);
            navItem.addView(navTitle);
            navItemContainer.addView(navItem);

            final int index = x;

            navItemContainer.setOnClickListener(v -> {
                isBackNav = false;
                selectNavItem(index);
            });

            addRippleEffect(navItem);

            navContainer.addView(navItemContainer);
        }
        addView(navContainer);
        // Initial selection

    }

    private void selectNavItem(int position) {


        if (selectedPosition == position && position != 100) {
            Log.d(TAG, "Item already selected");
            return;
        }

        int unselectedPosition = selectedPosition;

        if (position != 100) {
            selectedPosition = position;

            // Unselect the previous item
            RelativeLayout prevNavItem = getNavItem(unselectedPosition);
            if (prevNavItem != null) {
                prevNavItem.setBackground(null);
                ImageView prevNavIcon = getNavItemIg(unselectedPosition);
                if (prevNavIcon != null) {
                    prevNavIcon.setColorFilter(getNavItemColor(unselectedPosition));
                    ViewGroup.LayoutParams params = prevNavIcon.getLayoutParams();
                    params.width = navIconSize;
                    params.height = navIconSize;
                    prevNavIcon.setLayoutParams(params);
                }
                TextView prevNavTitle = getNavItemTx(unselectedPosition);
                if (prevNavTitle != null) {
                    prevNavTitle.setVisibility(View.GONE);
                }
            }
        }


        // Select the new item
        RelativeLayout currentNavItem = getNavItem(selectedPosition);
        if (currentNavItem != null) {
            currentNavItem.setBackground(createRdBackground(getNavItemColor(selectedPosition)));
            animateWidth(currentNavItem);
            ImageView currentNavIcon = getNavItemIg(selectedPosition);
            if (currentNavIcon != null) {
                currentNavIcon.setColorFilter(navItemSelectColor);
                ViewGroup.LayoutParams params = currentNavIcon.getLayoutParams();
                params.width = navIconSize + dpToPx(5);
                params.height = navIconSize + dpToPx(5);
                currentNavIcon.setLayoutParams(params);
            }
            TextView currentNavTitle = getNavItemTx(selectedPosition);
            if (currentNavTitle != null) {
                currentNavTitle.setVisibility(View.VISIBLE);
                animateTextView(currentNavTitle);
            }
        }

        // Update ViewPager2 if available
        if (viewPager2 != null && viewPager2.getCurrentItem() != position) {
            viewPager2.setCurrentItem(position, true); // Smooth scroll
        }
        if (unselectedPosition != 100){
            Log.d(TAG, "Selected Position: " + selectedPosition + ", Unselected Position: " + unselectedPosition);

            if (!isBackNav && isBackStack && backStackList != null) {
                backStackList.add(unselectedPosition);

                if (backStackList.size() >= 2) {
                    backStackList.remove(0);
                }
                Log.d(TAG, "Added position " + unselectedPosition + " to back stack.");
                Log.d(TAG, "Current back stack: " + backStackList.toString());

            }

        }

        if (navItemSelectedListener != null) {
            navItemSelectedListener.onItemSelected(selectedPosition);
            if (unselectedPosition != 100) {
                navItemSelectedListener.onItemUnselected(unselectedPosition);
            }

        }
    }





    // New method to link with ViewPager2
    public void setupWithViewPager2(@NonNull ViewPager2 viewPager) {
        this.viewPager2 = viewPager;

        setOnPageChangeListener(viewPager2);
        adjustPaddingForNavBar(viewPager2);
    }



    public void setFragments(FragmentManager fragmentManager, int pagerContainerInt, ArrayList<Fragment> fragments) {
        this.fragmentManager = fragmentManager;
        this.pagerContainerInt = pagerContainerInt;
        this.fragments = fragments;

        if (pagerContainerInt != -1) {
            viewPager2 = getRootView().findViewById(pagerContainerInt);
            NavPagerAdapter adapter = new NavPagerAdapter(fragmentManager, getContext(), fragments);
            viewPager2.setAdapter(adapter);

            setOnPageChangeListener(viewPager2);
            adjustPaddingForNavBar(viewPager2);
        }
    }

    public void setFragments(FragmentManager fragmentManager, ArrayList<Fragment> fragments) {
        this.fragmentManager = fragmentManager;
        this.fragments = fragments;

        if (pagerContainerInt != -1) {
            viewPager2 = getRootView().findViewById(pagerContainerInt);
            NavPagerAdapter adapter = new NavPagerAdapter(fragmentManager, getContext(), fragments);
            viewPager2.setAdapter(adapter);


            setOnPageChangeListener(viewPager2);

            adjustPaddingForNavBar(viewPager2);
        }

    }

    /**
     * Set bottom padding of a view equal to the height of this BottomNavBar
     * @param view The view which needs bottom padding
     */
    public void adjustPaddingForNavBar(View view) {
        if (view == null) return;

    }


    private void setOnPageChangeListener(ViewPager2 pager) {
        if (pager == null) return;

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (selectedPosition != position) {
                    isBackNav = false;
                    selectNavItem(position);
                }
            }
        });
    }

    public void setViewPager2(ViewPager2 viewPager2){
        this.viewPager2 = viewPager2;
    }

    // ViewPager2 getter
    public ViewPager2 getViewPager2(){
        return viewPager2;
    }



    private int getNavItemColor(int index) {
        if (index >= 0 && index < navItemColors.length) {
            return navItemColors[index]; // Return Default or Custom Color
        }
        return Color.GRAY; // Fallback Color
    }


    private void animateWidth(final View view) {
        if (view == null) return;
        ValueAnimator widthAnimator = ValueAnimator.ofInt(view.getWidth(), (int) (navItemWidth + dpToPx(15)));
        widthAnimator.setDuration(300);
        widthAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            view.setLayoutParams(params);
        });
        widthAnimator.start();

        // Animate back to original width
        ValueAnimator shrinkAnimator = ValueAnimator.ofInt((int) (navItemWidth + dpToPx(15)), (int) navItemWidth);
        shrinkAnimator.setDuration(300);
        shrinkAnimator.setStartDelay(300);
        shrinkAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            view.setLayoutParams(params);
        });
        shrinkAnimator.start();
    }

    private void animateTextView(TextView textView) {
        ObjectAnimator translationX = ObjectAnimator.ofFloat(textView, "translationX", -textView.getTranslationX(), 0f);
        translationX.setDuration(500);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
        alpha.setDuration(600);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationX, alpha);
        animatorSet.start();
    }

    private Drawable createBottomNavBg(int color) {
        float radius = dpToPx(15);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
        return drawable;
    }

    private Drawable createRdBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dpToPx(10));
        return drawable;
    }

    @SuppressLint("NewApi")
    private void addRippleEffect(View view) {
        float cornerRadius = dpToPx(10);
        float[] radii = new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                cornerRadius, cornerRadius, cornerRadius, cornerRadius};
        RoundRectShape roundedRect = new RoundRectShape(radii, null, null);
        ShapeDrawable mask = new ShapeDrawable(roundedRect);
        RippleDrawable rippleDrawable = new RippleDrawable(
                ColorStateList.valueOf(navItemSelectColor),
                view.getBackground(),
                mask
        );
        view.setForeground(rippleDrawable);
    }

    // Public methods
    public void setMenuResId(int menuResId) {
        this.menuResId = menuResId;
        loadMenuFromXml(menuResId);

    }
    public void setMenu(List<NavMenuItem> menuList) {
        this.menuList = menuList;
        createNavView(menuList);
    }

    public void setSelectedItem(int position) {
        isBackNav = false;
        selectNavItem(position);
    }


    public void setNavItemColors(int[] colors) {
        if (colors != null && colors.length > 0) {
            this.navItemColors = colors;
            createNavView(menuList);
        }
    }

    public void setOnNavItemSelectedListener(OnNavItemSelectedListener listener) {
        this.navItemSelectedListener = listener;
    }
    // --- Back Stack Methods ---

    /**
     * Handles the back navigation logic.
     * @return true if the back stack is empty and we are at the initial item (position 0),
     * indicating the activity should be closed.
     * false otherwise, as navigation was handled.
     */
    public boolean onBackNav() {
        isBackNav = true;

        if (backStackList == null) return true;

        if (getSelectedPosition() == 0 && backStackList.isEmpty()) {
            return true;
        }

        if (backStackList != null && !backStackList.isEmpty()) {
            int position = backStackList.get(backStackList.size() - 1);
            backStackList.remove(backStackList.size() - 1);
            selectNavItem(position);
        } else {
            selectNavItem(0);
            backStackList.clear();
        }

        return false;
    }

    /**
     * Initializes the back stack.
     * Call this method to enable back navigation functionality.
     */
    public void backStack(){
        Log.d(TAG, "backStack() called.");
        isBackStack = true;
        backStackList = new ArrayList<>();
    }


    // Getters for individual views

    public int getSelectedPosition() {
        return selectedPosition;
    }
    public RelativeLayout getNavItem(int position) {
        return (RelativeLayout) navContainer.findViewWithTag("nav_item_" + position);
    }

    public ImageView getNavItemIg(int position) {
        return (ImageView) navContainer.findViewWithTag("nav_icon_" + position);
    }

    public TextView getNavItemTx(int position) {
        return (TextView) navContainer.findViewWithTag("nav_title_" + position);
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public ArrayList<Fragment> getFragments() {
        return fragments;
    }

    public void setFragments(ArrayList<Fragment> fragments) {
        this.fragments = fragments;
    }

    // Listener Interfaces
    public interface OnNavItemSelectedListener {
        void onItemSelected(int position);
        void onItemUnselected(int position);
    }

    // Utility method
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }


    public static class NavMenuItem {
        private final int id;
        private final Drawable icon;
        private final String title;

        public NavMenuItem(int id, Drawable icon, String title) {
            this.id = id;
            this.icon = icon;
            this.title = title;
        }

        public int getId() {
            return id;
        }

        public Drawable getIcon() {
            return icon;
        }

        public String getTitle() {
            return title;
        }
    }


    // A custom adapter for ViewPager2 to handle fragments
    private static class NavPagerAdapter extends FragmentStateAdapter {
        private final ArrayList<Fragment> fragments;

        public NavPagerAdapter(FragmentManager fragmentManager, Context context, ArrayList<Fragment> fragments) {
            super(fragmentManager,  ((LifecycleOwner) context).getLifecycle());
            this.fragments = fragments;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}
