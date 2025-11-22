package org.woen.modules.scoringSystem.storage


import woen239.enumerators.Ball



class TerminateIntakeEvent()
class StorageGetReadyForIntakeEvent(
    var inputBall: Ball.Name = Ball.Name.NONE
)