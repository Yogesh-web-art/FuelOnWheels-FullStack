package com.example.fuelonwheelsapp.profile.orders;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.viewModels.FOWViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.Collections;

public class OrderListFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "OrderListFragment";

    public OrderListFragment() {
        // Required empty public constructor
    }

    public static OrderListFragment newInstance(String param1, String param2) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private FOWViewModel viewModel;
    private ShimmerFrameLayout mShimmerViewContainer;
    private ImageView back_button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FOWViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);
        mShimmerViewContainer = view.findViewById(R.id.shimmer_view_container);
        back_button = view.findViewById(R.id.order_list_back_btn);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        back_button.setOnClickListener(view1 -> {
            try {
                Navigation.findNavController(view1).navigate(R.id.action_orderListFragment_to_profileFragment);
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.order_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.setHasFixedSize(true);

        viewModel.getResponseAfterFetchingOrderList((orders, exception) -> {
            if (!isAdded()) return;
            if (mShimmerViewContainer != null) {
                mShimmerViewContainer.stopShimmerAnimation();
                mShimmerViewContainer.setVisibility(View.GONE);
            }
            if (exception == null && orders != null && !orders.isEmpty()) {
                ArrayList<Order> safeList = new ArrayList<>(orders);
                Collections.reverse(safeList);
                recyclerView.setAdapter(new OrderListAdapter(safeList, (view12, position) -> {
                    if (position >= 0 && position < safeList.size()) {
                        viewModel.setOrder(safeList.get(position));
                        try {
                            Navigation.findNavController(view12).navigate(R.id.action_orderListFragment_to_orderDetailFragment);
                        } catch (Exception e) {
                            Log.e(TAG, "Navigation error: ", e);
                        }
                    }
                }));
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "You haven't placed an order yet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mShimmerViewContainer != null) {
            mShimmerViewContainer.startShimmerAnimation();
        }
    }

    @Override
    public void onPause() {
        if (mShimmerViewContainer != null) {
            mShimmerViewContainer.stopShimmerAnimation();
        }
        super.onPause();
    }
}
