package org.woen.hotRun

import org.woen.threading.ThreadedTimers
import org.woen.utils.smartMutex.SmartMutex

class RobotState {
    companion object{
        private var _nullableInstance: RobotState? = null

        private val _instanceMutex = SmartMutex()

        val LAZY_INSTANCE: RobotState
            get() = _instanceMutex.smartLock {
                if (_nullableInstance == null) _nullableInstance = RobotState()

                return@smartLock _nullableInstance!!
            }

        fun restart() {
            _instanceMutex.smartLock {
                _nullableInstance = null
            }
        }
    }

    enum class GameState{
        AUTO,
        TELEOP,
        END_GAME
    }

    var currentGameState = GameState.AUTO
        private set

    private constructor(){
        HotRun.LAZY_INSTANCE.opModeInitEvent += {
            currentGameState = if(HotRun.LAZY_INSTANCE.currentRunMode == HotRun.RunMode.AUTO) GameState.AUTO else GameState.TELEOP
        }

        HotRun.LAZY_INSTANCE.opModeStartEvent += {
            ThreadedTimers.LAZY_INSTANCE.startTimer(ThreadedTimers.Timer(1.5 * 60.0) {
                currentGameState = GameState.END_GAME
            })
        }
    }
}