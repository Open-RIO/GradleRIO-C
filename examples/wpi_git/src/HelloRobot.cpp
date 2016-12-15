#include "WPILib.h"
#include "HelloRobot.hpp"

#include <iostream>

int add(int a, int b) {
    return a + b;
}

class Robot: public SampleRobot {
public:
    Robot() { }

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
        std::cout << add(1, 2) << std::endl;
    }
};

START_ROBOT_CLASS(Robot)