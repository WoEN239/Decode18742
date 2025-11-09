package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball
import woen239.enumerators.BallRequest
import woen239.enumerators.IntakeResult
import woen239.enumerators.RequestResult
import woen239.enumerators.ShotType



class TerminateIntakeEvent()
class TerminateRequestEvent()



class StorageIsReadyToEatIntakeEvent()
class BallWasEatenByTheStorageEvent()
class StorageFinishedIntakeEvent(
    var intakeResult: IntakeResult.Name
)



class StorageGiveSingleRequest(
    var ballRequest: BallRequest.Name
)
class StorageGiveSimpleDrumRequest()
class StorageGiveDrumRequest(
    var shotType: ShotType,
    var requestPattern: Array<BallRequest.Name>,
    var failsafePattern: Array<BallRequest.Name> = arrayOf()
)
class StorageGiveStreamDumbDrumRequest()



class StorageRequestIsReadyEvent(var shotNum: Int = 1)
data class StorageFinishedEveryRequestEvent(
    var requestResult: RequestResult.Name
)

class ShotWasFiredEvent()


class BottomOpticPareSeesSomethingEvent()
class MobileOutOpticPareSeesSomethingEvent()



class ColorSensorsSeeIntakeIncoming(
    var inputBall: Ball.Name = Ball.Name.NONE
)
class StorageGetReadyForIntake(
    var inputBall: Ball.Name = Ball.Name.NONE
)



class StorageOpenGateForShot()
class StorageCloseGateForShot()



class StorageLazyResume()
class StorageLazyPause()