#include "WPILib.h"
#include "MyLib.hpp" // my_library subproject, provides add(int, int)

#include <iostream>

class Robot: public SampleRobot {
public:
    Robot() { }

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
        std::cout << add(1, 2) << std::endl;
    }
};

START_ROBOT_CLASS(Robot)