<img src='/EOCV-Sim/src/main/resources/images/icon/ico_eocvsim_new.png' height='128px'>

![Java CI with Gradle](https://github.com/deltacv/EOCV-Sim/workflows/Build%20and%20test%20with%20Gradle/badge.svg)
[![Maven Publish & Create GitHub Release(s)](https://github.com/deltacv/EOCV-Sim/actions/workflows/release_ci.yml/badge.svg)](https://github.com/deltacv/EOCV-Sim/actions/workflows/release_ci.yml)

# Welcome!

EOCV-Sim (EasyOpenCV Simulator) is a straightforward way to test your pipelines in a 
simple user interface directly in your computer, simulating the EasyOpenCV library & a bit of
FTC SDK structure, allowing you to simply copy paste directly your pipeline code once you want to 
transfer it onto your robot!

<img src='doc/images/eocvsim_screenshot_1.png' width='75%' height='75%'>

## Introducing PaperVision

EOCV-Sim comes with a tool that will make it even easier to work on your computer vision pipelines.
PaperVision takes inspiration in Blender and Unreal Engine's blueprint system, allowing you to create pipelines by connecting nodes together.<br><br>
PaperVision is still in its early stages, but it's already a powerful tool that will make it easier to work on your pipelines.<br><br>
<b>Find it in the "PaperVision" tab in the simulator !</b>

<img src='/EOCV-Sim/src/main/resources/images/papervision.gif' height='300px'>

# Learn how to install and use the simulator in the [documentation here](https://docs.deltacv.org/eocv-sim)
### (Common users won't need to scroll down any further from here - the docs have everything you need)

# [Buy me a coffee](https://buymeacoffee.com/serivesmejia)

For the past 4 years I've been developing and maintaining learning tools for robotics kids to have a better understanding of programming and computer vision. Now that I've graduated from the robotics competition and I'm headed to college it is my goal to keep maintaining and improving these tools for future generations to keep learning, completely cost-free and open source. Your donation in [buy me a coffee](https://buymeacoffee.com/serivesmejia) will help me maintain those goals through the following years as life gets busier. It is completely voluntary, but it will help ensure these tools remain accessible and continuously improved for future robotics enthusiasts. Thank you for considering supporting my passion for robotics!

\- Sebastian Erives, deltacv's main dev

# Acknowledgments

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.

<img src='https://www.yourkit.com/images/yklogo.png' width='20%' height='20%'>

# Compatibility

Since OpenCV in Java uses a native library, which is platform specific, the simulator is currently limited to the following platforms:

* Windows x86_64 (tested)
* Windows x86 (untested)
* MacOS x86_64 (tested)
* MacOS AARCH64/Apple Silicon (untested)
* Linux x86_64 (tested for Ubuntu 20.04)
* Linux ARMv7 & ARMv8 (partially tested in Raspbian but not officially endorsed)<br/>

## Downloading and documentation

Follow the steps in [this page](https://docs.deltacv.org/eocv-sim/downloading-eocv-sim) to download the sim. The rest of the documentation can also be found [there](https://deltacv.gitbook.io/eocv-sim/).

## Adding EOCV-Sim as a dependency for plugin development

### NOT FOR FTC SDK USAGE, please follow the documentation above if you're a normal user not aiming to develop for EOCV-Sim

   ### Gradle
   ```groovy
   repositories {
       maven { url 'https://jitpack.com' } //add jitpack as a maven repo
       mavenCentral() //add maven central as a maven repo
   }
   
   dependencies {
      implementation 'com.github.deltacv:EOCV-Sim:3.3.2' // add the EOCV-Sim dependency, make sure to replace for the latest version
   }
   ```
   
   ## Maven
   
   Adding the jitpack maven repo
   ```xml
    <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
   ```
   
   Adding the EOCV-Sim dependecy
   ```xml
    <dependency>
	    <groupId>com.github.deltacv</groupId>
	    <artifactId>EOCV-Sim</artifactId>
	    <version>3.3.2</version>
	</dependency>
   ```

# Contact
For bug reporting or feature requesting, use the [issues tab](https://github.com/deltacv/EOCV-Sim/issues) in this repository.

Join the [deltacv discord server](https://discord.gg/A3RMYzf6DA) !

# Change logs

### Formerly, EOCV-Sim was hosted on a [personal account repo](https://github.com/serivesmejia/EOCV-Sim/). Released prior to 3.0.0 can be found there for historic purposes.


## [v4.1.1 - Internal EventHandler API rework](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.1.1)
- This is the 35th release for EOCV-Sim
    - Complete internal rework of the EventHandler system
        - Eliminates listener execution blocking during registration and removal
        - Guarantees listeners added during a tick are deferred to the next tick
        - Improves thread-safety without relying on full execution locks
        - Reduces contention and avoids unnecessary allocations in hot paths
  - PaperVision is still absent in this release and will be added in the next patch, awaiting for Maven Central publication

## [v4.1.0 - Major Plugin API Update](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.1.0)
- This is the 34th release for EOCV-Sim
    - Major update to the plugin API, implementing a stable interface that abstracts away EOCV-Sim's internal workings and allows for better compatibility across versions
    - Moves some APIs and utilities into the Common module, including the plugin API
      - Plugin developers are now expected to depend on the Common module instead of EOCV-Sim directly, as EOCV-Sim's internal APIs may change without notice
    - PaperVision is absent in this release and will be added in the next patch, awaiting for Maven Central publication

## [v4.0.3 - PaperVision launch hotfix](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.0.3)
- This is the 33nd release for EOCV-Sim
    - Updates PaperVision to [1.0.10](https://github.com/deltacv/PaperVision/releases/tag/v1.0.10)
    - Fixes issues [#136](https://github.com/deltacv/EOCV-Sim/issues/136) and [#137](https://github.com/deltacv/EOCV-Sim/issues/137)
    - Adds embedded PaperVision as a separate shadow jar file instead of being an integrated dependency to fix a hard to trace native crash

## [v4.0.2 - Embedded PaperVision v1.0.7](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.0.2)
- This is the 32nd release for EOCV-Sim
    - Adds HTTP input source to support MJPEG streams as an input source. Limelight cameras can stream to EOCV-Sim !
    - Updates to [PaperVision v1.0.7](https://github.com/deltacv/PaperVision/releases/tag/v1.0.4)
    - Addresses the changes made to the Vision module in the FTC SDK v11.0
    - PaperVision is now embedded in EOCV-Sim's binary instead of being downloaded on startup

## [v4.0.1 - Plugin Auto-Updater](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.0.1)
- This is the 31th release for EOCV-Sim
    - Adds a plugin auto-updater, which will automatically check for updates on the plugins you have pulled using repository.toml
    - Adds a dialog prompt when another EOCV-Sim instance is detected, instead of silently failing
    - Updates to [PaperVision v1.0.4](https://github.com/deltacv/PaperVision/releases/tag/v1.0.4)
## [v4.0.0 - PaperVision is here !](https://github.com/deltacv/EOCV-Sim/releases/tag/v4.0.0)
- This is the 30th release for EOCV-Sim
    - Introducing PaperVision ! A tool bundled in the simulator that will make it even easier to work on your computer vision pipelines.
    - Auto-accepts superaccess on trusted plugins, this reduces the UI clutter, especially for PaperVision
    - Improved several dialogs, adds a better "create workspace" dialog & a welcome dialog that is shown when the sim is opened for the first time
    - New modernized application icon
    - @qwertychouskie made their first contribution: "macOS: Add application name, use system light/dark mode in titlebar" and "macOS: Use standard Settings & About entries in menu". Thank you !

## [v3.8.4 - Maven Central Migration](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.8.4)
- This is the 29th release for EOCV-Sim
    - Migrates all of EOCV-Sim's artifacts to the maven central repository (bye bye JitPack)
    - Bugfixes:
        - Adds error checking at multiple points of the sim to handle simple edge cases that shouldn't cause an app crash

## [v3.8.3 - Plugin Classloader major optimization](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.8.3)
- This is the 28th release for EOCV-Sim
    - Improves plugin classloader classpath loading by caching and resource reusage, this brings an extremely noticeable performance boost for plugins that load a lot of class files from different Maven dependencies in a short amount of time (I'm looking at you Javalin)
    - Stops SuperAccessDaemon from creating a separate log file

## [v3.8.2 - Synchronization bug fixes](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.8.2)
- This is the 27th release for EOCV-Sim
    - Internal changes:
        - Opens streamer field access of StreamableOpenCvPipeline
    - Bugfixes:
        - Fixes incorrect buffer sharing in org.opencv.android.Utils, which caused funny flickering
        - Restructures cache files into a single folder to avoid cluttering the plugins folder

## [v3.8.1 - Shadow jar merge service files hotfix](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.8.1)
- This is the 26th release for EOCV-Sim
    - Bugfixes:
        - Fixes shadow jar, uses mergeServiceFiles to fix failing to pull plugins from a maven repository

## [v3.8.0 - Major Plugin System Rework](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.8.0)
- This is the 25th release for EOCV-Sim
    - Changelog
        - Updates AprilTagDesktop to 2.1.0-C, enabling support for Linux AARCH64.
        - Implements a system that allows for plugins to be downloaded from a maven repository and loaded into the simulator.
        - New repository.toml file that contains the list of repositories to download plugins from and the maven coordinates to download.
        - Adds a new dialog to manage the loaded plugins and check the output of the plugin system.
        - Rewrites SuperAccess verification to be handled by a separate JVM process, ensuring that the main process is not compromised by malicious code.
        - Implements a plugin signature verification system to allow for authors to sign their plugins and ensure that they are not tampered with.
        - The signature is stored on a custom format in the plugin jar, the signing authorities are pulled from a public key database.
        - When making a SuperAccess request, the user is warned if the plugin is not signed or if the signature is invalid.
        - Adds a KeyGeneratorTool and PluginSigningTool to allow for easy generation of keys and signing of plugins respectively.
        - Internal changes:
            - Adds a PolymorphicAdapter class to allow for easy serialization while retaining type information.
            - Improvements to the handling of JavaProcess.

## [v3.7.1 - Better FTC VisionPortal support & Plugin System Fixes](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.7.1)
- This is the 24th release for EOCV-Sim
    - Changelog
        - Update skiko to 0.8.15
        - Fixes Typeface.DEFAULT_BOLD and Typeface.DEFAULT_ITALIC to actually work
        - Adds a stub no-op implementation for the FTC SDK Gamepad class into OpMode
        - Adds android.opengl.Matrix implementation and matrices from the FTC SDK
        - Adds navigation classes from the FTC SDK (AngleUnit, AxesOrder, AxesReference, etc)
        - Adds the ConceptAprilTagLocalization, ConceptVisionColorLocator and ConceptVisionColorSensor samples
        - Reimplements Telemetry to EOCVSimTelemetryImpl with stubs for Telemetry#talk
        - Internal changes:
            - Plugin virtual filesystems now use the name and author in the TOML to generate the fs name
            - Allows to specify JVM args in JavaExec
            - Rename some internal classes
            - Better handling of Pipeline/OpMode tab switching

### [v3.7.0 - FTC SDK 10.1 & Refined Plugin System](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.7.0)
- This is the 23nd release for EOCV-Sim
    - Changelog
        - Addresses the changes made in the FTC SDK 10.1 for the 2024-2025 season:
            - Adds new OpenCV-based VisionProcessors (which may be attached to a VisionPortal in either Java or Blocks) to help teams implement color processing via computer vision in the INTO THE DEEP game
        - Internal changes:
            - Fixes virtual filesystem by scrapping jimfs and using a zip filesystem.
            - Implements virtualreflect api for variable tuner to abstract away the reflection api and allow for future diverse implementations.
            - Implements StreamableOpenCvPipeline to allow for diverse implementations of streaming different Mat stages of a pipeline to a target.
    - Bugfixes:
        - Fixes exception loop when an exception is thrown from pipeline init

### [v3.6.0 - Plugin System & Into the Deep AprilTags](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.6.0)
- This is the 22nd release for EOCV-Sim
    - Changelog
        - Addresses the changes made in the FTC SDK 10.0 for the 2024-2025 season:
            - Adds the INTO THE DEEP AprilTag Library with accurate tag locations.
        - Brand-new plugin system for EOCV-Sim, allowing for easy integration of new features and libraries from the community (documentation pending, oops).
        - Adds the ability to load plugins from the user's filesystem.
        - Implements virtual filesystem for plugins, allowing for a sandboxed environment away from the user's filesystem.
        - Implements whitelist and blacklist for plugins and dynamically loaded pipelines, allowing for a sandboxed environment.

### [v3.5.3 - Android Paint constructor fix](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.5.3)
   - This is the 21st release for EOCV-Sim
     - Bug fixes:
       - Addresses #110 by properly cloning underlying skiko Paint in android.graphics.Paint(Paint) constructor

### [v3.5.2 - CenterStage AprilTags](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.5.2)
   - This is the 20th release for EOCV-Sim
     - Changelog
       - Addresses the changes made in the FTC SDK 9.0 for the 2023-2024 season:
         - Adds the CENTERSTAGE AprilTag Library with accurate tag locations
       - Exceptions thrown from OpenCvCamera/VisionPortal's attached user code created within OpModes is now reported as part of EOCV-Sim's output dialog to allow for easier debugging

### [v3.5.1 - FTC SDK 9.0](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.5.1)
   - This is the 19th release for EOCV-Sim
     - Changelog
       - Addresses the changes made in the FTC SDK 9.0 for the 2023-2024 season:
         - Fields in AprilTagDetection and AprilTagPose(ftc/raw) objects are now final
         - VisionPortal builder method setCameraMonitorViewId() has been renamed to setLiveViewContainerId() and enableCameraMonitoring() has been renamed to enableLiveView()

### [v3.5.0 - New VisionPortal and VisionProcessor API](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.5.0)
   - This is the 18th release for EOCV-Sim
     - Changelog
       - Addresses the changes made in the FTC SDK 8.2 to prepare for the 2023-2024 season:
         - EOCV-Sim's Viewport implementation has been changed to one using Skiko (Skia) rendering - to address new features implemented in [EasyOpenCV v1.7.0](https://github.com/OpenFTC/EasyOpenCV/releases/tag/v1.7.0)
         - The VisionPortal & VisionProcessor interfaces have been implemented onto EOCV-Sim - VisionProcessors are treated just like OpenCvPipelines and are automatically detected by the sim to be executed from the user interface.
         - In order to use the VisionPortal API, OpModes have been added onto the simulator - a new "OpMode" tab on the user interface has been added to address this addition. NOTE: OpModes are only limited to use VisionPortal APIs, other FTC SDK apis such as hardware DcMotor have not been implemented.
         - A new public API for android.graphics has been adding onto the simulator, translating android.graphics API called by the user into Skiko calls, adding compatibility to the new features in [EasyOpenCV v1.7.0](https://github.com/OpenFTC/EasyOpenCV/releases/tag/v1.7.0) related to canvas drawing.
         - AprilTagProcessor has also been implemented straight from the SDK, allowing its full API to be used and attached to a VisionProcessor - [see this example OpMode](https://github.com/deltacv/EOCV-Sim/blob/dev/TeamCode/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/ConceptAprilTagEasy.java).
       - AprilTagDesktop plugin has been updated to match [EOCV-AprilTag-Plugin v2.0.0](https://github.com/OpenFTC/EOCV-AprilTag-Plugin/releases/tag/v2.0.0)
       - Support for Apple Silicon Macs has been added to AprilTagDesktop
       - Several quality of life upgrades to the UI
      - Bug fixes: 
        - Fixes issues related to pipeline and input source selection - UI components now exclusively react to user interactions as opposed to past versions where changes triggered by EOCV-Sim were picked up as user-made and caused several issues

### [v3.4.3 - M1 Mac OpenCV Support](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.4.3)
   - This is the 17th release for EOCV-Sim
     - Changelog
       - Update OpenCV to 4.5.5, adding M1 support
       - Disables OpenIMAJ webcams when not using x86_64

### [v3.4.2 - AprilTags fixes & Mac support](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.4.2)

   - This is the 16th release for EOCV-Sim
 
      - Changelog:      
        - Bumps apriltags to 1.2.0, providing Mac support and removing OpenCV dependency on native code to hopefully remove crashes
      - Bug fixes:
        - Fixes path of crash reports to use corresponding separator
	
### [v3.4.1 - Custom OpenCV native support](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.4.1)

   - This is the 15th release for EOCV-Sim
 
      - Changelog:      
        - Adds command line argument for specifying the path to the opencv native library, falls back to normal loadLocal in case of a failure

### [v3.4.0 - Webcam stability improvements](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.4.0)

   - This is the 14th release for EOCV-Sim
 
      - Changelog:      
        - Adds option for selecting between OpenCV and OpenIMAJ webcam drivers, with a fallback to OpenCV in case OpenIMAJ fails to load
        - Support for webcam rotation to adjust depending on the direction on which the webcam could be mounted
        - Migrated logging to slf4j with log4j backend (non log4shell version)
        - Improved various dialog windows across the sim to be a little more visually pleasing
        - Full telemetry implementation rewrite by using the FTC SDK interface and implementation (with minor modifications)
        - Scrapped webcam-capture
      - Bug fixes: 
        - Fixes crashes related to camera sources and OpenIMAJ loading
        - Fixes prolonged lags when opening the camera source creation dialog
        - Fixes classloader and jar packaged to use correct zip file separators (/ instead of File.separator)

### [v3.3.2 - Better compiler support](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.3.2)

   - This is the 13th release for EOCV-Sim
 
      - Changelog:
        - Provides support for compiling in any Java virtual machine by using the Eclipse ecj compiler if a JDK compiler isn't found.
        - Provided a fallback webcam driver in case OpenIMAJ fails to load.
      - Bug fixes:
        - Fixes camera source sizes serialization where the height was replaced by the width
        
### [v3.3.1 - Common module hotfix](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.3.1)

   - This is the 12th release for EOCV-Sim
 
      - Bug fixes:
        - Includes the common module in the jitpack build

### [v3.3.0 - AprilTags on windows](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.3.0)

   - This is the 11th release for EOCV-Sim
 
      - Changelog:
        - Support for AprilTags in windows! Thanks so much to Windowoes (NPE) for providing us with the Windows native library as well!
        - Improves webcam support using names instead of indexes when opening cameras. A legacy mechanism was added for stored cameras still using indexes
        - The UI for webcam source creation was also improved, now it only shows the supported resolutions of a webcam instead of two free textboxes for possibly unsupported resolutions
        - Adds the FTC SDK @Disabled annotation for preventing pipelines from being registered when looking up in the classpath

### [v3.2.0 - Partial AprilTag support!](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.2.0)

   - This is the 11th release for EOCV-Sim
 
      - Changelog:
        - Improved pipeline error handling and error output gui
        - Partial support AprilTags! Only on Linux for now, based on the [EOCV-AprilTag-Plugin](https://github.com/OpenFTC/EOCV-AprilTag-Plugin) by Windwoes (NPE). Thanks so much to him for making the plugin and providing us with the Linux native library!
        - Updated the OpenPnP OpenCV package to the 4.5.1 following the OpenCV update that was made in Android EasyOpenCV. 
        - ...EasyVision?

     - Bugfixes:
        - Fixed the camera source creation dialog when there's not any webcam plugged and improved state handling
        - Fixes a crash with workspaces when a pipeline contains an inner class and it's placed on a different package as the `package;` statement specifies.
        - Fixes the IntelliSense of the gradle workspace template by including the openpnp opencv implementation line.

### [v3.1.0 - Better Error Handling](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.1.0)

   - This is the 10th release for EOCV-Sim
 
      - Changelog:
        - Improved pipeline error handling and error output gui
        - Build output was improved and unified with the pipeline error output gui
        - Added a SplashScreen with the EOCV-Sim logo while the sim loads
        - Settings for changing the max FPS of the video recordings and pipelines, and the max pipeline processing time before it's considered "stuck on processFrame"
        - Improved camera source creation dialog by providing a list of the available cameras
        - Added file locking so that two EOCV-Sim instances can't exist at the same time
        - Updated links to reflect the change to the deltacv organization

### [v3.0.0 - Compiling on the fly! Yay!](https://github.com/deltacv/EOCV-Sim/releases/tag/v3.0.0)

   - This is the 9th release for EOCV-Sim

      - Changelog:
        - Runtime building! The sim now supports building pipelines on the fly, which allows for more quick and efficient testing. (Running the sim with a JDK is required for this feature to work, since normal JREs don't include a compiler to use)
        - Workspaces & VS Code is the new (and recommended) way of developing pipelines. A VS Code workspace template can be created from the sim, see the usage explanation for more details.
        - A file watcher was implemented, so when any modification happens in the current workspace under the "source" or "resource" folders specified in the `eocvsim_workspace.json`, a new build will be automatically triggered every 8 seconds.
        - VS Code can be executed by the sim if the current system has the `code` command. It is triggered by manually opening it in the top menu bar or when creating a VS Code workspace.
        - Files can now be drag and dropped into the sim to add them as Input Sources. The sim will automatically open a create dialog depending on the file extension.
        - Added a "Workspace" menu under the top menu bar, which contains the new features regarding the runtime compiling.
        - The UI now has smoother icons, by using a smoothing option on Java swing which makes them look a little nicer (but not much).
        - Current pipeline state is now stored and reestablished if a restart happens.
        - When a build is finished, the simulator tries reinitializes the currently selected pipeline if it exists, to ensure the changes were applied. Or it falls back to the `DefaultPipeline` if the old pipeline doesn't exist anymore, it also saves the state of the old pipeline and tries to apply the snapshot of the pipeline before it was reinitialized if the names of the old and new classes match.
        - The sim now uses a `.eocvsim` folder under the user directory to store its files, to avoid annoying the user with unwanted files now that the runtime compiling exists and it has to store the build output somewhere. If the user has previously run an older version of eocv sim which created `eocvsim_sources.json` and/or `eocvsim_config.json` under the user home directory, it automatically migrates them to the new folder. 
        - Builds created by IntelliJ Idea (the common programming style) are now considered as "dev". This helps to distinguish between official & published builds created in a CI workflow and local builds, when an issue happens and it's reported.
        - The sim compiling target was changed back to Java 8, since this is one of the most widely used versions and we weren't really using many Java 9 features that couldn't have been replaced or handle different. This is also more convenient and provides better support for users directly downloading the jar and executing it.

      - Bugfixes:
        - Fixed issues with the source selector regarding to selection when a modification or error happens. When a new source is added, it's automatically selected. And when a source is deleted, the previous source in the list is selected
        - Fixed the color picker cursor size on non-windows systems
        - Fixed pause not working when a tunable field that uses a combo box in the UI is included.
        - Fixed an (apparently random, but it's just the garbage collector being weird) null pointer exception with `Enum` fields

      - Internals:
        - Improved event handlers to be more idiomatic and less weird. Bye bye KEventListener!
        - Improved some messy parts of the internal code and logic

### [v2.2.1 - JVM crashing hotfix](https://github.com/serivesmejia/releases/tag/v2.2.0)
 
   - This is the 8th release for EOCV-Sim
   
      - Changelog:
        - Removed "Java memory" message in the title since it's practically useless for the end user
        - Updated to Gradle 7.0 for Java 16+ support (#25)

      - Bugfixes:
        - Fixed JVM crashing error caused by releasing all mats in a MatRecycler finalization (#26)
        - Improved memory usage by deleting unused BufferedImageRecyclers, memory is now slightly freed when allocating or recycling buffered images of different sizes (which means that the memory usage is reduced a little bit when zooming in the viewport)

### [v2.2.0 - Variable Tuner Upgrade](https://github.com/serivesmejia/releases/tag/v2.2.0)
 
   - This is the 7th release for EOCV-Sim

      - Changelog:
 
        - Pipelines now have a timeout all of the three methods so that the main loop doesn't get compromised due to a "stuck" pipeline (using kotlin coroutines)
  	    - processFrame has a timeout of 4.2 seconds
  	    - init is executed in the same scope as processFrame, when it has to be called, the timeout is doubled (16.4)
  	    - When either processFrame or init methods timeout, the sim automatically falls back to the default pipeline and discards any frame that the old timeouted pipeline could return.
  	    - onViewportTapped is still called from the U.I Thread, but it now has a timeout of 4.2 seconds too
        - Added EnumField which handles the type Enum (accepts all classes of type enum, including the ones declared by the user)
        - Major improvements to the variable tuner, added new features for color picking, tuning with sliders, configuration... See [usage explanation](https://github.com/serivesmejia/EOCV-Sim/blob/master/USAGE.md) for further details.
        - GUI improvement: Dropped some external dialogs in favor of simple "popups" for more practicality
        - Internals:
    		- Continued rewrite to kotlin
    		- Splitted visualizer class components into different classes
    		- Improved EventHandler doOnce listeners

### [v2.1.0 - Video Update](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v2.1.0)

   - This is the 6th release for EOCV-Sim

      - Changelog: 

        - Added support for VideoSources! You can now input your pipeline with a moving video (*.avi format is the most supported and tested, other codecs might depend on the OS you're using)
        - Added support for video recording, accessible at the bottom of the pipeline selector. Save format is AVI
        - Added a new TunableField type: RectField, which handles the OpenCV type "Rect" (might be useful for rect pipelines 👀)
        - Improved uncaught exception handling and added a crash report generator
        - Added support for more themes from FlatLaf
        - Added new config option to change the output video recording size
        - Added support for EOCV's TimestampedOpenCvPipeline
        - Internals:
    		- Major rewrite to kotlin! (Still mostly Java but that might change soon)
    		- A bit of code cleaning and restructuring 

### [v2.0.2 - TaskBar hotfix](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v2.0.2)
      
  - This is the 5th release for EOCV-Sim.
      
      - Bugfixes:
        
        - Fixes UnsupportedOperationException with the TaskBar API in some operating system
            
### [v2.0.1 - BooleanField hotfix](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v2.0.1)
      
  - This is the 4th release for EOCV-Sim.

      - Bugfixes:
      
        - Fixes ArrayIndexOutOfBoundsException when initial value of a boolean field was true which would make the sim enter into a frozen state.

### [v2.0.0 - Major Update](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v2.0.0)
      
 - This is the 3rd release for EOCV-Sim.

      - Changelog:
      
      	- Gradle is now used as the main build system
        - Added variable tuner for public non-final supported fields in the pipeline, accessible on the bottom part of the image viewport.
        - Pipeline pause and resume option to save resources, pauses automatically with image sources for one-shot analysis
        - Top Menu bar containing new features/convenient shortcuts:
          - Save Mat to disk option in File submenu
          - Restart feature in File submenu
          - Shortcut for creating input sources under File -> New -> Input Source
          - Settings menu under Edit submenu
          - "About" information screen under Help submenu
          - Appereance themes via the FlatLaf library, selectable in the settings window
        - Telemetry now is passed to the pipeline via the constructor rather than an instance variable, check usage explaination for further details
        - Mat visualizing into the viewport is now handled in another thread to improve performance
        - Pipeline FPS are now capped at 30
        - Zooming viewport is now supported, using mouse wheel while holding Ctrl key
        
      - Bugfixes:
        
        - Removed call to the gc in the main loop due to performance issues
        - Fixed BufferedImage mem leak by recycling previously used buffered images and trying to flush them
        - Some internal code cleaning & reestructuration
        - Fixed issues with native lib loading (mostly on Mac) with the OpenCV package provided by OpenPnP
     
### [v1.1.0 - Telemetry Update](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v1.1.0)
      
 - This is the 2rd release for EOCV-Sim.
      
      - Changelog:

        - Added a Telemetry implementation displayed in the UI. Replicates the FTC SDK one, it can be used directly in pipelines.
        - Added an option to define the CameraSource resolution when creation.
        - Added MacOS support (thnx Noah)
        - Changed default resolution to 320x280 everywhere since it is the most commonly used in EOCV
        - Native libs are now downloaded by the simulator from another GitHub repo to avoid bloating the repository with heavy files
        - Java libraries, such as classgraph, opencv and gson are now delivered in compiled jars to improve compile times
       
      - Bug fixes:
       
        - Fixed a bug where the InputSources would return a BGR Mat instead of RGB, which is the type EOCV gives.
        - Regarding the last point, the visualizer now expects for the given mats to be RGB
        - Improved general IO error handling everywhere, from file accessing to input sources reading, so that the simulator doesn’t enter in a freeze state if any IO related operation fails
        - Improved multi threading handling for changing pipelines and inputsources.
        - Fixed issue in Linux where the buttons would be moved to an incorrect position when resizing out and then trying to resize back to the original size
 
 
### [v1.0.0 - Initial Release](https://github.com/serivesmejia/EOCV-Sim/releases/tag/v1.0.0)
      
 - Initial EOCV-Sim release.
      
