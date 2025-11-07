javac -d classes/ -cp "lib/*" src/*/*.java


jar cvf mon_framework.jar -C classes/ .

cp mon_framework.jar /home/elyance/Documents/S5/'Mr Naina'/Test/src/main/webapp/WEB-INF/lib