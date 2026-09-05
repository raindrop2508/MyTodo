package com.gordon.mypotato

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gordon.mypotato.data.AppDatabase
import com.gordon.mypotato.data.repository.RoomPomodoroRepository
import com.gordon.mypotato.data.repository.RoomTaskRepository
import com.gordon.mypotato.databinding.ActivityMainBinding
import com.gordon.mypotato.domain.OrphanPomodoroSettlement
import com.gordon.mypotato.domain.PomodoroPhase
import com.gordon.mypotato.domain.PomodoroTimerLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 暂时强制为日间模式
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 底部导航栏（Bottom Navigation）与 Jetpack Navigation 组件进行关联
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        if (savedInstanceState == null) {
            checkOrphanPomodoroSessions()
        }
    }

    private fun checkOrphanPomodoroSessions() {
        lifecycleScope.launch {
            val prompt = withContext(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(this@MainActivity)
                val settlement = OrphanPomodoroSettlement(
                    RoomPomodoroRepository(database.pomodoroSessionDao()),
                    RoomTaskRepository(database.taskDao(), database.taskStepDao())
                )
                settlement.preparePrompt()
            } ?: return@launch

            if (!prompt.isFocusPhase) {
                showBreakOrphanDialog(prompt)
            } else {
                showFocusOrphanDialog(prompt)
            }
        }
    }

    private fun showBreakOrphanDialog(prompt: OrphanPomodoroSettlement.Prompt) {
        AlertDialog.Builder(this)
            .setTitle(R.string.pomodoro_orphan_break_title)
            .setMessage(getString(R.string.pomodoro_orphan_break_message, prompt.taskTitle))
            .setCancelable(false)
            .setPositiveButton(R.string.pomodoro_orphan_ok) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    createSettlement().discard(prompt.session)
                }
            }
            .show()
    }

    private fun showFocusOrphanDialog(prompt: OrphanPomodoroSettlement.Prompt) {
        val stepSuffix = prompt.stepTitle?.let {
            getString(R.string.pomodoro_orphan_step_suffix, it)
        } ?: ""
        val phaseLabel = phaseLabel(prompt.session.getPhase())
        val durationText = PomodoroTimerLogic.formatDurationSec(prompt.elapsedFocusSec)
        val message = if (prompt.cleanedOlderCount > 0) {
            getString(
                R.string.pomodoro_orphan_message_multiple,
                prompt.cleanedOlderCount + 1,
                prompt.taskTitle,
                stepSuffix,
                phaseLabel,
                durationText
            )
        } else {
            getString(
                R.string.pomodoro_orphan_message_single,
                prompt.taskTitle,
                stepSuffix,
                phaseLabel,
                durationText
            )
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.pomodoro_orphan_title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(R.string.pomodoro_orphan_discard) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    createSettlement().discard(prompt.session)
                }
            }

        if (prompt.elapsedFocusSec > 0L) {
            builder.setPositiveButton(R.string.pomodoro_orphan_keep) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    createSettlement().keepFocusDuration(prompt.session, prompt.elapsedFocusSec)
                }
            }
        } else {
            builder.setPositiveButton(R.string.pomodoro_orphan_ok) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    createSettlement().discard(prompt.session)
                }
            }
        }
        builder.show()
    }

    private fun createSettlement(): OrphanPomodoroSettlement {
        val database = AppDatabase.getDatabase(this)
        return OrphanPomodoroSettlement(
            RoomPomodoroRepository(database.pomodoroSessionDao()),
            RoomTaskRepository(database.taskDao(), database.taskStepDao())
        )
    }

    private fun phaseLabel(phase: PomodoroPhase): String {
        return when (phase) {
            PomodoroPhase.FOCUS -> getString(R.string.pomodoro_phase_focus)
            PomodoroPhase.SHORT_BREAK -> getString(R.string.pomodoro_phase_short_break)
            PomodoroPhase.LONG_BREAK -> getString(R.string.pomodoro_phase_long_break)
        }
    }
}
