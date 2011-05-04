# This file is used to build and automatically test sonews

import os
env = Environment()

env['ENV']['LANG'] = 'en_GB.UTF-8'
env['JAVACFLAGS']    = '-source 1.6 -target 1.6'
env['JAVACLASSPATH'] = '/usr/share/java/junit4.jar:/usr/share/java/glassfish-mail.jar:/usr/share/java/servlet-api-2.5.jar:/usr/share/java/jchart2d.jar:classes'

# Build Java classes
classes = env.Java(target='classes', source=['org/sonews/'])
test_classes   = env.Java(target='classes', source=['test'])

# Setting dependencies
Depends(test_classes, classes)

