package com.example.safeguardapp.Group;

import android.os.Bundle;
import android.text.GetChars;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safeguardapp.LogIn.LoginPageFragment;
import com.example.safeguardapp.MainActivity;
import com.example.safeguardapp.R;
import com.example.safeguardapp.RetrofitClient;
import com.example.safeguardapp.UserRetrofitInterface;
import com.example.safeguardapp.data.model.Group;
import com.example.safeguardapp.data.repository.GroupRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupFragment extends Fragment {
    RetrofitClient retrofitClient;
    UserRetrofitInterface userRetrofitInterface;
    private GroupRepository repository;
    private RecyclerView groupListView;
    private Button addGroupBtn;
    private String currentGroupUuid;
    private String childID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = GroupRepository.getInstance(requireContext());
        initializeView(view);
        setupListeners();

        repository.getGroupListStream().observe(getViewLifecycleOwner(), groupList -> {
            groupListView.setAdapter(new GroupAdapter(groupList, new GroupAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(Group group) {
                    currentGroupUuid = group.getUuid(); // 클릭한 그룹의 UUID를 저장
                    childID = group.getId();
                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.containers, GroupSettingFragment.newInstance(currentGroupUuid, childID));
                    transaction.commit();
                }
            }));
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로 가기 시 실행되는 코드
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                // 이동 시에는 이미 생성된 mapFragment를 사용하여 교체
                transaction.replace(R.id.containers, ((MainActivity) requireActivity()).mapFragment);
                transaction.commit();

                BottomNavigationView navigationView = requireActivity().findViewById(R.id.bottom_navigationview);
                navigationView.setSelectedItemId(R.id.map);
            }
        });
    }

    private void initializeView(View view) {
        addGroupBtn = view.findViewById(R.id.add_group_btn);
        groupListView = view.findViewById(R.id.recycler_view);
        getChildID();
    }

    private void setupListeners() {
        addGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.containers, new AddGroupPopupFragment());
                fragmentTransaction.commit();
            }
        });
    }

    private static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupItemViewHolder> {
        private final List<Group> groupList;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Group group);
        }

        public GroupAdapter(List<Group> groupList, OnItemClickListener listener) {
            this.groupList = groupList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public GroupItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_group, parent, false);
            return new GroupItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupItemViewHolder holder, int position) {
            Group group = groupList.get(position);
            holder.bind(group, listener);
        }

        @Override
        public int getItemCount() {
            return groupList.size();
        }

        static class GroupItemViewHolder extends RecyclerView.ViewHolder {
            public MaterialButton button;

            public GroupItemViewHolder(@NonNull View itemView) {
                super(itemView);
                button = itemView.findViewById(R.id.button);
            }

            public void bind(final Group group, final OnItemClickListener listener) {
                button.setText(group.getName());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick(group);
                    }
                });
            }
        }
    }

    private void getChildID() {
        retrofitClient = RetrofitClient.getInstance();
        userRetrofitInterface = RetrofitClient.getInstance().getUserRetrofitInterface();

        String memberID = LoginPageFragment.saveID;
        GetChildIDRequest memberIDDTO = new GetChildIDRequest(memberID);

        Call<GetChildIDResponse> call = userRetrofitInterface.getChildID(memberIDDTO);
        call.enqueue(new Callback<GetChildIDResponse>() {
            @Override
            public void onResponse(Call<GetChildIDResponse> call, Response<GetChildIDResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GetChildIDResponse.Child> children = response.body().getChildren();
                    // 응답 처리 로직
                    for (GetChildIDResponse.Child child : children) {
                        Log.e("POST","Name: " + child.getName());
                    }
                } else {
                    // 오류 처리 로직
                    System.err.println("Response error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GetChildIDResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
