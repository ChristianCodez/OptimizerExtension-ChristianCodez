Link to YouTube Video:


Instructions for Running My Project:

You shouldn't have to but just to be safe run the following first thing ->  mvn clean compile assembly:single  

I built an Optimizer, you run it just like you would any other HW
For me it's
  -> .\win_mypl.bat -m OPTIMIZER .\examples\hw4_static_2.mypl

I also added the option in MyPL to run
  -> .\win_mypl.bat -m IR-O .\examples\hw4_static_2.mypl
This runs the IR, but this IR has had the AST put through the optimizer first

Run this to see the normal IR, really big difference
  -> .\win_mypl.bat -m IR .\examples\hw4_static_2.mypl

Run this for a complete usage of all my tests-> mvn test 

Just FYI, the OptimizerPerformanceTests were completely AI generated so as such I take no credit for those. They also do not run, funnily enough. 
I wanted to see if I could get tests that show a difference in memorary usage and time but they didn't quite work out...


