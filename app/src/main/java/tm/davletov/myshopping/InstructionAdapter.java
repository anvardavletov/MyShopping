package tm.davletov.myshopping;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class InstructionAdapter extends FragmentStateAdapter {
    public InstructionAdapter(FragmentActivity fa) {
        super(fa);
    }

    @Override
    public Fragment createFragment(int position) {
        return new InstructionFragment();
    }

    @Override
    public int getItemCount() {
        return 3; // 3 страницы инструкции
    }
}