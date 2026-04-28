package com.gordon.mypotato.ui.tasks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentTaskDetailBinding

class TaskDetailFragment : Fragment(R.layout.fragment_task_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentTaskDetailBinding.bind(view)
        val taskId = arguments?.getString("taskId").orEmpty()
        val taskTitle = arguments?.getString("taskTitle").orEmpty()
        Log.d(TAG, "onViewCreated taskId=$taskId taskTitle=$taskTitle")
        binding.tvDetailTitle.text = getString(R.string.task_detail_title)
        binding.tvDetailTaskName.text = getString(R.string.task_detail_task_name, taskTitle)
        binding.tvDetailTaskId.text = getString(R.string.task_detail_task_id, taskId)
        // TODO: 接入任务详情完整信息查询与展示逻辑（标题、描述、步骤、番茄记录）。
    }

    private companion object {
        private const val TAG = "TaskDetailFragment"
    }
}
