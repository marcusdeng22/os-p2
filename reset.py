import os

for file in os.listdir(os.path.dirname(os.path.realpath(__file__))):
	#remove all .class files
	if os.path.isfile(file) and ".class" in file:
		print("removing file:", file)
		os.unlink(file)

#reset file.txt
open("file.txt", "w").close()
