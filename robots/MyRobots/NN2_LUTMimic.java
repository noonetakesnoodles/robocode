/*-> INTRO TO THIS BOT AND OVERALL PROJECT <-
NN2_LUTMimic is our first bot that applies neural network (NN or net) techniques to reinforcement 
learning(RL). The purpose of coding NN2 is to code a basic skeleton for future bots to develop 
NN-specific behaviours and parameters, and we can make sure that the code works by comparing 
behaviour between LUTTrackfire and NN2. Like its name suggests, NN2_LUTMimic mimicks a LUT-based 
robot (mainly LUTTrackfire) by replicating its many parameters, such as state and action parameters, 
instead of designing parameters that employ neural net advantages. 
  
ROBOCODE is a program designed for learning the basics of java. The goal is to code a bot that 
competes with others in an arena. The coder has no direct influence during the fight - instead, 
all the strategies and fight mechanics employed by the robot are coded before the fight takes 
place. The coder is free to employ any tactics he/she wants to win - as long as the bot adheres to 
the rules of the game. (Robocode ReadMe: http://robocode.sourceforge.net/docs/ReadMe.html	
Robocode wiki: http://robowiki.net)

The premise for this project is that reinforcement learning can be used to improve robot combat
adaptibility against all types of enemy robot behaviours, by learning counters to their behaviour 
in real time. 

REINFORCEMENT LEARNING, RL, refers to the method by which a machine selects an action. 
The decision is made by choosing the action that maximizes a conceptualized reward: the bot 
performs an action within a measurable environment, and rewards itself based upon the results of 
said action. The reward alters the likelihood of re-performing the same action again in the same 
environment: winning moves can be recreated, and poor moves can be avoided. This ability to learn 
during the actual battle gives the robot combat adaptibility.

Neural network (NN) is a computation network that solves problems by employing complexity, 
approximation, and trial and error. It imitates biological neurons in design through making both 
inputs and outputs to the system to be neuronal nodes, which, like a biological neuron network, 
connects to other neurons and forms a network with the connections. The net has the ability to 
solve problems by acting sort of like an equation - a very complex equation with changeable 
coefficients, and through trial and error of repeatedly trying to achieve correct outputs with 
various given inputs, the network can create a relatively accurate model of the system. Complexity 
is often a benefit for correct modeling (much like a linear line is a less efficient model of a 
higher order equation, whereas higher order equations can model lower ones relatively easily). The 
network often includes other nodes called 'hidden nodes' to add complexity. 
It can also be structured in a complex manner (such as multidimensional nodal connections). The 
simplest structure of a NN - which is used for our bots - is a 2-dimensional net consisting of 3 
1-D layers: a 1D layer of input nodes connected to a layer of hidden nodes, which connects to both 
the input layer as well as a layer of output nodes. These connections have changeable values 
associated to them (aka 'weights'), and is the engine behind the net's ability to adjust its 
approximation.

Neural net is used here for RL, just like LUT, with one main distinction being that NN can only 
estimate the final value of actions. In exchange, NN allows more inputs, and promotes greater input
variety. In a LUT, each selection of inputs has a corresponding value: for a system with 2 inputs, 
with respectively 3 and 4 possible values, the total number of values to be stored is 
3x4 = 12. This is a problem for robocode: For the robot to remember behaviours after combat, the LUT 
method requires a list of QVals to be stored in a file in between executions. Robocode limits all files 
to a size of 200kBs, and it doesn't take long before a robot with multiple inputs to reach 200kBs. NN on 
the other hand estimates the QVals through calculations, and stores only the values associated with the 
neural net connections, or 'weights'. This is a much smaller set of values. Weights describe the strength 
of the connections between nodes in a neural net, and it can be any real value. By requiring only 
weights to be stored, NN allows a much greater selection of inputs to be used. Even continuous 
input ranges can be used, which is impossible for LUT.  
  
-> SUMMARY OF HOW NN2 WORKS <-
Each turn of battle, an event is triggered. The event does the following:
	1. Obtain information about the environment.
	2. Once every 4 turns, RL will take place (allows robot to complete movement actions that takes a few turns. After testing, 4 is best).
	   
	   in RL: 	1) Convert environmental information into inputs that are usable by net, including states and rewards.
	   			2) Convert several key NN variables from 'current' tense into 'previous' tense.
 		 		3) Choose best action for current cycle - forward propagation using current states.
   				4) Perform Q value function to create adjustment quantities for net.
   				5) Correct weights - back propagate using previous layer values
   				6) Perform action based on current cycle's maxQ. 
       non-RL turns: 1) maintain scanlock on enemy.
	3. Perform actions mandatory per turn, such as maintaining scanner lock on enemy.
  
NN2 consists of several data analyzing fxns:
	1. Logging of important sections in templog.txt
	2. Logging of Qvals per cycle of RL in qVals.txt
	3. Logging of errors calculated during Q function in saveErrorForActions.dat. These errors 
	   are used to determine if Qvalue is converging.
	4. Errors during import/export are printouts in o.stream in addition to being logged.
  
Check worklog.txt and planning.txt in MyRobots dir for bot's past, present and future.
  
Anyways here's wonderwall.
*/
package MyRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class NN2_LUTMimic extends AdvancedRobot{
	
	/*
	 * RULES FOR CHANGING STATE ACTION VECTOR(SAV):
	 * 1. State/Action related finals must be changed accordingly.
	 * 2. NN input dimension related finals must be changed accordingly.
	 * 3. If states changed, change the state accordingly in generateCurrentStateVector()
	 * 4. If actions changed, change the actions in doAction_Q() AND all action for loops T_T
	 * 5. In NN2, no states or actions are expected to be changed. But for future related bots, changing the dimensions of the weights will likely cause error during input.
	 *      Note that error and use that as prompt to input a randomized weight selection.
	 */
	
	/*
	 * STEPS FOR IMPORTING/EXPORTING A NEW FILE
	 * 1. Create the txt/dat file in the robotname.data directory (or try to export and the game will create directory and file for u)
	 * 2. Add a configmask code for the file. ie. CONFIGMASK_FILETYPE_FILESHORTNAMEHERE = 0x0..0, and make the addition in the comment paragraph above.
	 * 3. Create a string that refers to the filename in the "STRINGS used for importing or extracting files" section.
	 * 4. Add flag_imported for the specific file.
	 * 5. Add permission flag for file if applicable.
	 * 6. Add fileSettings_fileshortname for the specific file.
	 * 7. Add data storage array and total lines of data if applicable.
	 * 8. If file requires import, write an import fxn call by following the correct format in the function run().
	 * 9. If the file requires export, write an export in the proper locations, in fxns onBattleEnded() or/and onWin() + onDeath().
	 * 10. Add actual code.
	 * 11. In importData(): 
	 * 		update available config section in fxn's @brief comments
	 * 		add fileSettings log line in (1) beginning of function,
	 *									 (2) dump, 
	 *									 (3) and end of function.
	 * 		add section in else if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_blah) == CONFIGMASK_FILETYPE_blah) && (flag_blahImported == false) ) {, which includes:
	 *				NOTE: for these sections, we may refer to other codes.
	 * 				1. a section on if wrong file name
	 *				2. section for if zeroconfig
	 *				3. section for if not zeroconfig
	 *				4. fileSettings_fileShortName = fileSettings_temp
	 *				5. file_--imported = true
	 * 12. In exportData():
	 * 		update available config section in fxn's @brief comments (zeroing section not necessary)
	 *		add fileSettings log line in beginning of function
	 *		add if condition for file.
	 *		add section in else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ), which includes:
	 *				1. LOG indications that the code has arrived in/leaving this section.
	 *				2. streamname.println(fileSettings_fileShortName)
	 *				3. codes for export
	 *				4. flag_--imported = false
	 */
	
	/**
	 * ===================================FINALS (defines)====================================================================================
	 */
	//variables for the q-function. Robot will currently NOT change critical q-function coefficients (alpha, gamma, epsilon) mid-fight.
	//alpha describes the extent to which the newly acquired information will override the old information.
    private static final double alpha = 0.1;
    //gamma describes the importance of current rewards
    private static final double gamma = 0.8;                
    //epsilon describes the degree of exploration
    private static final double epsilon = 0.05; 				 
    
    //policy:either greedy or exploratory (both are Q-learning, perhaps possibility of implementing SARSA in the future?)
    private static final int greedy = 0;
    private static final int exploratory = 1;
    //Joey: SARSA is unusable - currently incomplete.
    //SARSA uses the reward gained by performing action 2 to update reward in action 1.
    private static final int SARSA = 2;
    
    
    /* 
     * CONFIGMASK: a list of numbers that describe what's in the file, and provide some limited instructions to the reader (which is the import fxn).  
     * 			  - contains 2 bytes (4 hex digits)
     * 			  - functions will use AND conditions to evaluate if the settings in the file match the mask. 
     */
    // _ _ _ _  _ _ _ _  _ _ _ _  _ _ _ _ 
    //   MSnib	filename(2 nibs)   LSnib
    // MSnib is the first nibble (4 bits) - currently used to verify the config setting is present in the file.
    // the 2nd and 3rd nibbles are used for recognizing specific files
    // LSnib is used for file-specific settings.
    //
    // Current available config settings:
    //     				stringTest: 16400 (0x4010)
    // 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
    //     				WL:			16448 (0x4040), zero WL = 16449 (0x4041)
    //					NN weights: 16512 (0x4080), zeroing = 16513 (0x4081)
    //					QVals:		16640 (0x4100), zeroing = 16641 (0x4101)
    //					BPErrors:	16896 (0x4200), zeroing = 16897 (0x4201)
    private static final short CONFIGMASK_ZEROINGFILE  =				0x0001;
    private static final short CONFIGMASK_VERIFYSETTINGSAVAIL = 		0x4000;
    private static final short CONFIGMASK_FILETYPE_stringTest =			0x0010;
    private static final short CONFIGMASK_FILETYPE_LUTTrackfire =		0x0020;
    private static final short CONFIGMASK_FILETYPE_winLose = 			0x0040;
    private static final short CONFIGMASK_FILETYPE_weights =			0x0080;
    private static final short CONFIGMASK_FILETYPE_QVals =				0x0100;
    private static final short CONFIGMASK_FILETYPE_BPErrors = 			0x0200;
    
    //IMPORT/EXPORT status returns.
    
    private static final int SUCCESS_importData = 						0x00;
    private static final int SUCCESS_exportData = 						0x00;
    private static final int SUCCESS_importDataWeights =				0x00;
    private static final int SUCCESS_exportDataWeights =				0x00;
    
    private static final int ERROR_1_import_IOException = 				1;
    private static final int ERROR_2_import_typeConversionOrBlank = 	2;
    private static final int ERROR_3_import_verification = 				3;
    private static final int ERROR_4_import_wrongFileName_stringTest =	4;
    private static final int ERROR_5_import_wrongFileName_WL =			5;
    private static final int ERROR_6_export_cannotWrite =				6;
    private static final int ERROR_7_export_IOException =				7;
    private static final int ERROR_8_import_dump =						8;
    private static final int ERROR_9_export_dump =						9;
    private static final int ERROR_10_export_mismatchedStringName =		10;
    private static final int ERROR_11_import_wrongFileName_LUT = 		11;
    private static final int ERROR_12_importWeights_IOException = 		12;
    private static final int ERROR_13_importWeights_typeConversionOrBlank = 13;
    private static final int ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden = 14;
    private static final int ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput = 15;
    private static final int ERROR_16_exportWeights_IOException = 		16;
    private static final int ERROR_17_importWeights_flagImportedFalse = 17;
    private static final int ERROR_18_exportWeights_flagImportedTrue = 	18;
    private static final int ERROR_19_import_wrongFileName_weights =	19;
    private static final int ERROR_20_import_weights_wrongNetSize =		20;
    private static final int ERROR_21_import_wrongFileName_QVals =		21;
    private static final int ERROR_22_import_wrongFileName_BPErrors =	22;
    
    /*
	 * NN STATEACTION VARIABLES for stateAction ceilings (for array designs and other modular function interactions).
	 * 
	 */
    //Action-related finals.
    //- One concern with the complexity of the actions a robot can perform is the amount of calculation time spent in forward propagation.
    //- Each possible action requires one forward propagation.
    //- As of now, the No. times NN needs to forward propagate per round = 4 * 2 * 3 = 24
    private static final int input_action0_moveReferringToEnemy_possibilities = 4; //0ahead50, 0ahead-50, -90ahead50, -90ahead-50
    private static final int input_action1_fire_possibilities = 2;    //1, 3
    private static final int input_action2_fireDirection_possibilities = 3;    //-10deg, 0, 10deg
    private static final int numActionContainers = 3;   
    private static final int numActions = input_action0_moveReferringToEnemy_possibilities 
    									  * input_action1_fire_possibilities
    									  * input_action2_fireDirection_possibilities;
    
    //State related finals.
    private static final int input_state0_myPos_possibilities = 5;    //center, left, right, top, bottom (cannot be undiscretized) 
    private static final int input_state1_myHeading_originalPossilibities = 4;    //0-89deg, 90-179, 180-269, 270-359
    private static final int input_state2_enemyEnergy_originalPossibilities = 2;    //>30, <30
    private static final int input_state3_enemyDistance_originalPossibilities = 3;    //<150, <350, >=350
    private static final int input_state4_enemyDirection_originalPossibilities = 3;    //head-on (still (abs <30 || >150), left (<0 relative dir w/ positive velo || >0 with negative velo), right (<0 dir w/ negative velo || >0 with positive velo)
    private static final int numStateContainers = 5;
    
    //NN neuron parameters.
    private static final int numInputBias = 0;
    private static final int numHiddenBias = 1;
    private static final int numOutputBias = 0; //Actual code disregards possibility of output bias by starting loops relating to output at 0 referring to first output instead of bias.
    private static final int numHiddenNeuron = 4;
    private static final int numInputsTotal = ( numInputBias + numActionContainers + numStateContainers ); 
    private static final int numHiddensTotal = ( numHiddenBias + numHiddenNeuron );
    private static final int numOutputsTotal = 1;
    
    //NN activation function choices
    private static final boolean binaryMethod = true;
    private static final boolean bipolarMethod = false;
 
    
    /**
     * STRINGS used for importing or exporting files =========================================== 
     */
    String strStringTest = "stringTest.dat";    
    String strLUT = "LUTTrackfire.dat";
    String strWL = "winlose.dat";
    String strSA = "stateAction.dat"; 
    String strBPErrors = "BPErrors.txt" ;
    String strWeights = "weights.dat";
    String strLog = "templog.txt";
    String strQVals = "qVals.txt";
    
    /**
     * FLAGS AND COUNTS ===========================================================================
     */
    
    
    //<<<<<< CRITICAL FLAGS >>>>>>
    //
    //Flags that allow certain parts of code or data to be used!
    //
    //flag that prompts user to use offline training data from LUT. (ONLY applicable for NN2)
    private static boolean flag_useOfflineTraining = true;
    //flag to permit log file to be imported/exported.
    private static boolean flag_recordLog = true;
    //flag used to permit program to record QVals
    private static boolean flag_recordQVals = false;
    //flag used to permit program to record BP round errors
    private static boolean flag_recordBPErrors = false;
    
    
    
    //DEBUG_ALL flags. Each allows printouts written for specific functions. DEBUG_ALL will print out all.
    private final static boolean DEBUG_ALL = true; 
	private final static boolean DEBUG_run = false;
	private final static boolean DEBUG_onScannedRobot = false;
	private final static boolean DEBUG_analysis = false;
	private final static boolean DEBUG_onBattleEnded = false;
	private final static boolean DEBUG_onDeath = false;
	private final static boolean DEBUG_onWin = false;
//	private final static boolean DEBUG_learnThisRound = false;
	private final static boolean DEBUG_obtainReward = false;
	private final static boolean DEBUG_generatePrevs = false;
	private final static boolean DEBUG_generateCurrentStateVector = false;
//	private final static boolean DEBUG_RL_and_NN = false;
	private final static boolean DEBUG_MULTI_forwardProp = false; //can be used to debug the multiple fxns encompassed by FP.
	private final static boolean DEBUG_getAllQsFromNet = false;
	private final static boolean DEBUG_forwardProp = false;
	private final static boolean DEBUG_getMax = false;
	private final static boolean DEBUG_qFunction = false;
	private final static boolean DEBUG_MULTI_backProp = true;
	private final static boolean DEBUG_prepareBackProp = false;
	private final static boolean DEBUG_backProp = false;
//	private final static boolean DEBUG_resetReward = false;
    private final static boolean DEBUG_doAction_Q = false;
//	private final static boolean DEBUG_doAction_notLearning = false;
//	private final static boolean DEBUG_doAction_mandatoryPerTurn = false;
//	private final static boolean DEBUG_importDataWeights = false;
//	private final static boolean DEBUG_exportDataWeights = false;
    private final static boolean DEBUG_MULTI_file = false; //logs from all functions that contribute directly to moving/editing files, which include more than import/export fxns.
    private final static boolean DEBUG_import = false;
    private final static boolean DEBUG_export = false;
    

    

    
    
    // Flags used in data imp/exp fxns.
    //		Purposes:
    // 		 - prevents overwrite, and protects against wrong file entries
    //		 - data for a particular file must be exported before importing for the same file occurs again.
    // 		false == only imports can access; true == only exports can access.
    //		ALWAYS initialize these as false.
    private boolean flag_stringTestImported = false;
    private boolean flag_LUTImported = false;
    private boolean flag_WLImported = false;
    private boolean flag_weightsImported = false;
    private boolean flag_QValsImported = false;
    private boolean flag_BPErrorsImported = false;

    // printout error flag - used to record the return value of functions.
    // initialized to 0, which is no error.
    private int flag_fileAccessReturn = 0;

    /**
     *  OTHER VARIABLES USABLE BY THIS ROBOT'S CLASS FUNCTIONS ==============================================================================
     */
    
    // weights connecting between input and hidden layers. calculated using definitions defined above.
    private static double[][] arr_wIH 
        = new double
        [numInputsTotal]
        [numHiddensTotal] 
        ;
    
    // weights connecting between hidden layer to output.
    private static double[][] arr_wHO
    	= new double
    	[numHiddensTotal]
    	[numOutputsTotal]
    	;
    
    // temp vars for importing/exporting files: config settings for the external files, stored in the first line of .dat
    // PART OF THE CONFIGMASK SERIES.
    private short fileSettings_temp = 0;
    private short fileSettings_stringTest = 0;
    private short fileSettings_LUT = 0; 
    private short fileSettings_WL = 0;
    private short fileSettings_weights = 0;
    private short fileSettings_log = 0;
    private short fileSettings_QVals = 0;
    private short fileSettings_BPErrors = 0;
    
    // reward and reward calculation vars.
    private double reward_normalized = 0.0;
    private double energyDiffCurr = 0.0;
    private double energyDiffPrev = 0.0;

    //activation method used for binary and bipolar methods.
    private boolean activationMethod = bipolarMethod; 

    //chosen policy. greedy or exploratory or SARSA
    //possibility of allowing robot to change these patterns
    private static int policy = greedy; //SAR
    private static int learningRate = 4; //learningAlgo is run every 4 ticks. 

    //enemy bot information
    private double enemyDistance = 0.0;
    private double enemyHeadingRelative = 0.0;
    private double enemyHeadingRelativeAbs = 0.0;
    private double enemyVelocity = 0.0;
    private double enemyBearingFromRadar = 0.0;
    private double enemyBearingFromGun = 0.0;
    private double enemyBearingFromHeading = 0.0;
    private double enemyEnergy = 0.0;
    
    //my bot information
    private double myHeading = 0.0; 
    private double myEnergy = 0.0;
    private double myPosX = 0.0;
    private double myPosY = 0.0;
    
    //misc battle information
    private int turn = 0;
    
    //used to update WL export
    private int totalFights = 0;
    private int[] battleResults = new int [520000];
    private int currentBattleResult = 0;
	
    //vars and arrays used for debugging purposes - storage of data, total lines of data
    private static String[] LOG = new String [520000];
    private static int lineCount = 0;
    private static double[] arr_QVals = new double [520000];
    private static int totalQValRecords = 0;
    private static double[] arr_BPErrors = new double [520000];
    private static int totalBPErrorsRecords = 0;

    //class vars used to store function call time
    // private long aveDuration = 0;
    // private static long totalDuration = 0;
    // private static int durationCount = 0;
    
    //vars that store current and previous stateAction vectors
    private double currentStateActionVector[] = new double [numInputsTotal];
    private double prevStateActionVector[]    = new double [numInputsTotal]; //might not be used 

    //Q-var storages.
    //- "Y" and "Q" pretty much refers to the same thing, but to make it easier to understand when coding, we use "Y" for the BP calculations, and Q for Q fxn.

    private double[] Q_prev_new = new double[numOutputsTotal];
    
    
    /**  
     * Neural net stuff  
     */
	private double [] y	   = new double[numOutputsTotal];		// Array to store values of Y
    // analysis rate //Joey: ask Andrea about use of this.
	// private double lRate = 0.05; 			
	//value of momentum //Joey: research the used of momentum for optimal values. 
	private static final double momentum = 0.1;  		
	
	// arrays used for momentum
	private double [][] wIH_past  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights for Past.
	private double [][] wIH_next  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights.
	private double [][] wHO_past  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights for Past.
	private double [][] wHO_next  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights.

	//bias for hidden initialized as value 1
	// private static final int valInputBias = 0;
    private static final int valHiddenBias = 1;
    // private static final int valOutputBias = 0;
    
    //@@@@@@@@@@@@@@@ RUN & EVENT CLASS FUNCTIONS @@@@@@@@@@@@@@@@@    
    
    /**
     * @name: 		run
     * @purpose:	1. Initializes robot colour
     * 				2. Clears log from previous session in case it used up all alloted space.
     * 				3. Imports weights, win-lose records, and any other files desired by the user.
     * 				4. Enters infinite scan mode until enemy robot is scanned, which triggers event onScannedRobot. This is where
     * 				   most of the robot's logic begins.
     * @param:		n
     * @return:		n
     */
   
    public void run() {
        
        // Sets Robot Colors.
        setColors();
        
        out.println("@I have been a dodger duck (robot entered run)"); 
        if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
        	LOG[lineCount++] = "@I have been a dodger duck (robot entered run)";
        }
        
        // Import data. Change imported filename below
        
        //always clears log from previous session in case it used up all allowed harddrive. (robocode allows for 200kB of external data per robot)
        
        if (flag_recordLog) {// only record log in file when flagged.
	        fileSettings_log += CONFIGMASK_ZEROINGFILE; //changes file setting to zeroing on next open. strLog is the next file to be opened, and it is typically so large that it is only once per run.	        
	        flag_fileAccessReturn = exportData(strLog);
	        if (flag_fileAccessReturn != SUCCESS_importData) {
	        	out.println("ERROR @run blankingWeights: " + flag_fileAccessReturn);
	        }
	        fileSettings_log -= CONFIGMASK_ZEROINGFILE; //resets file setting to post-zeroing.
        }
        
        if (flag_recordQVals) { //only recordQVals when flagged.
	        flag_fileAccessReturn = importData(strQVals);
	        if(flag_fileAccessReturn != SUCCESS_importData) {
	        	out.println("ERROR @run QVals: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
	        		LOG[lineCount++] = "Error @run Qvals: " + flag_fileAccessReturn;
	        	}
	        }
        }
        
        if (flag_recordBPErrors) { //only recordBPErrors when flagged.
	        flag_fileAccessReturn = importData(strBPErrors);
	        if(flag_fileAccessReturn != SUCCESS_importData) {
	        	out.println("ERROR @run BPErrors: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
	        		LOG[lineCount++] = "Error @run BPErrors: " + flag_fileAccessReturn;
	        	}
	        }
        }
        
        flag_fileAccessReturn = importDataWeights(); //always importDataWeights.
        if (flag_fileAccessReturn != SUCCESS_importDataWeights) {
        	out.println("ERROR @run weights: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
            	LOG[lineCount++] = "ERROR @run weights: " + flag_fileAccessReturn;
            }
        }
        
        flag_fileAccessReturn = importData(strWL); //Joey: consider not storing WL on default.
        if (flag_fileAccessReturn != SUCCESS_importData) {
        	out.println("ERROR @run WL: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
            	LOG[lineCount++] = "ERROR @run WL: " + flag_fileAccessReturn;
            }
        }

            
        //set gun and radar for robot turn separate gun, radar and robot (robocode properties). 
        setAdjustGunForRobotTurn(true);
    	setAdjustRadarForGunTurn(true);	
    	setAdjustRadarForRobotTurn(true);

    	// anything in infinite loop is initial behaviour of robot
        for(;;){
        	setTurnRadarRight(20);
    		execute();					//from "AdvancedRobot" to allow parallel commands. 
        }
         
    }
    
    /**
     * @name: 		onBattleEnded
     * @purpose: 	1. 	Export
     * 				2.	Exports weights from memory to .txt file, which stores weights 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * @param:		1.	BattleEndedEvent class from Robot
     * @return:		n
     */
    public void onBattleEnded(BattleEndedEvent event){

    	flag_fileAccessReturn = exportDataWeights();	
        if (flag_fileAccessReturn != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onBattleEnded weights: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file|| DEBUG_onBattleEnded || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onBattleEnded weights: " + flag_fileAccessReturn;
        	}
        }
        
        if (flag_recordLog) { // log is exported only in onBattleEnded because it is typically too large to .... //Joey: test if onBattleEnded and onDeath/onWin runs twice.
	    	flag_fileAccessReturn = exportData(strLog); //export log LAST to prevent oversize for other critical files.					
	        if (flag_fileAccessReturn != SUCCESS_exportData) {
	        	out.println("ERROR @onBattleEnded Log: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_onBattleEnded || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onBattleEnded Log: " + flag_fileAccessReturn;
	        	}
	        }
        }
    }
    
    /**
     * @name: 		onDeath
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * 				2.  Sets a terminal reward of -100 
     * @param:		1.	DeathEvent class from Robot
     * @return:		n
     */
    public void onDeath(DeathEvent event){
    	
    	currentBattleResult = 0;    					//global variable that stores a 0 for loss. 
    	
    	if (flag_recordQVals) {
	    	flag_fileAccessReturn = exportData(strQVals);
	        if(flag_fileAccessReturn != SUCCESS_exportData) {
	        	out.println("ERROR @onDeath QVals: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onDeath QVals: " + flag_fileAccessReturn;
	        	}
	        }
    	}
        
    	if (flag_recordBPErrors) {
	    	flag_fileAccessReturn = exportData(strBPErrors);
	        if(flag_fileAccessReturn != SUCCESS_exportData) {
	        	out.println("ERROR @onDeath BPErrors: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onDeath BPErrors: " + flag_fileAccessReturn;
	        	}
	        }
    	}
    	
    	flag_fileAccessReturn = exportData(strWL);					//"strWL" = winLose.dat
        if (flag_fileAccessReturn != SUCCESS_exportData) {
        	out.println("ERROR @onDeath WL: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onDeath WL: " + flag_fileAccessReturn;
        	}
        }
        
        flag_fileAccessReturn = exportDataWeights();
        if (flag_fileAccessReturn != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onDeath weights: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onDeath weights: " + flag_fileAccessReturn;
        	}
        }
        
        
    }
    
    /**
     * @name: 		onWin
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * 				2.  Sets a terminal reward of +100 
     * @param:		1.	WinEvent class from Robot
     * @notes: 		can not call learningLoop() in onWin or onDeath because these are final events. 
     * @return:		n
     */    
	public void onWin(WinEvent e) {
    	currentBattleResult = 1;
    	
    	if (flag_recordQVals) {
	    	flag_fileAccessReturn = exportData(strQVals);
	        if( flag_fileAccessReturn != SUCCESS_exportData) {
	        	out.println("ERROR @onWin QVals: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onWin QVals: " + flag_fileAccessReturn;
	        	}
	        }
    	}
    	
    	if (flag_recordBPErrors) {
	    	flag_fileAccessReturn = exportData(strBPErrors);
	        if(flag_fileAccessReturn != SUCCESS_exportData) {
	        	out.println("ERROR @onWin BPErrors: " + flag_fileAccessReturn);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onWin BPErrors: " + flag_fileAccessReturn;
	        	}
	        }
    	}
        
        flag_fileAccessReturn = exportData(strWL);
        if (flag_fileAccessReturn != SUCCESS_exportData) {
        	out.println("ERROR @onWin WL: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_onWin || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onWin WL: " + flag_fileAccessReturn;
        	}
        }
        
    	flag_fileAccessReturn = exportDataWeights();
        if (flag_fileAccessReturn != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onWin weights: " + flag_fileAccessReturn);
        	if(DEBUG_MULTI_file || DEBUG_onWin || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onWin weights: " + flag_fileAccessReturn;
        	}
        }
	}

	/**
     * @name:		onScannedRobot
     * @brief:		This function is called by the game when the enemy is located by the scanner. It is the only function that will obtain info on the current game 
     * 				 environment, and decide what the robot will perform during this turn.
     * @purpose:	1. determines:
     * 					- my heading
     * 					- my position: x and y
     * 					- my energy
     * 					- enemy heading
     * 					- enemy velocity
     * 					- enemy bearing
     * 					- enemy distance
     * 					- enemy energy
     * 					- current turn
     * 				2. call analysis fxn to determine the next move for this turn.
     * @param:		ScannedRobotEvent event
     * @return:		none, but updates:
     * 				1. getGunBearing
     * 				2. enemyDistance
     */

	public void onScannedRobot(ScannedRobotEvent event){
		
		myHeading = (int)getHeading();
		myPosX = (int)getX();
		myPosY = (int)getY();
		myEnergy = (int)getEnergy();
		enemyHeadingRelative = (int)normalRelativeAngleDegrees(event.getHeading() - getGunHeading());
		enemyHeadingRelativeAbs = Math.abs(enemyHeadingRelative);
		enemyVelocity = (int)event.getVelocity();
		enemyBearingFromRadar = (double)myHeading + event.getBearing() - getRadarHeading();
		enemyBearingFromGun = (double)myHeading + event.getBearing() - getGunHeading();
		enemyBearingFromHeading = event.getBearing();
		enemyDistance = (int)event.getDistance(); 
		enemyEnergy = (int)event.getEnergy();
		turn = (int)getTime();
		
    	analysis();
    }


    //@@@@@@@@@@@@@@@ OTHER INVOKED CLASS FUNCTIONS @@@@@@@@@@@@@@@@@
    
    /** 
     * @name: 		setColors()
     * @purpose: 	Sets robot colour.  
     * @param: 		none
     * @return: 	none
     */
    public void setColors() {
        setBodyColor(Color.pink);
        setGunColor(Color.green);
        setRadarColor(Color.blue);
        setScanColor(Color.white);
        setBulletColor(Color.red);
    }
    
    /**
     * @name:		analysis
     * @purpose:	1. Analyze all environmental and self values.
     * 				2. Decide if reinforcement learning via NNet should be used this round
     * 					2a. Perform NN action, or
     * 					2b. Perform standard action.
     * 				3. Perform mandatory action.
     * @NNet:		Our neural net online training involves the following:
     * 				0. (not in NN) Determine if learning should happen this turn. Fxn learnThisRound returns a boolean.
     * 				1. Calculate how well we have done since the last NN update. (The time between this NN access and previous NN update is called a round)
     * 				2. Store the previous state and action (currentSAV -> prevSAV), and previous Q value.
     * 				3. Use information from the environment to determine the current state.
     * 				4. Decide best action based on max Q, which is calculated using all possible actions from current state.
     * 				5. Perform QFunction (detailed further in function).
     * 				6. Use result of QFunction to correct net.
     * 				7. Reset rewards. IE: all events affect reward only once unless further emphasized by events other than onScannedRobot. (NONE YET)
     * 				8. Perform chosen action. (learning-specific as well as those mandatory per turn).
     * @param:		n
     * @return:		n
     */
    /* Training online detailed by A (important, several points repeated from @NNet): 
     *  step (1) - need a vector of just states "copyCurrentSV into prev SV". 
        step (2) - get weights array - neural net do forwardpropagation for each action in the "CurrentSV"  , remembering all outputs "Y" in an array
        step (3) - choose maximum "Y" from array 
        step (4) - call qFunction() below using prevSAV as qOld and qNew is the SAV with max Y (chosen in step (3)) 
        		   - with return being the qOld_(new) 
        step (5) - with prevSAV (inputs) and qOld_new (correct target)  and qOld (calculated output), run backpropagation & save weights 
        step (6) - save error for graph
        step (7) - repeat steps 1-6 using saved weights from backpropagation to feed into NN for step (2)  
     */
    public void analysis() {
    	if (learnThisRound()){
    		
    		//this DEBUG_ALL fxn is related to onScannedRobot fxn, but placed here so that we can log it only when RL is firing.
    		if(DEBUG_onScannedRobot || DEBUG_MULTI_forwardProp || DEBUG_backProp || DEBUG_ALL || DEBUG_MULTI_file) {
    			LOG[lineCount++] = " ";
        		LOG[lineCount++] = "@@@ TURN " + turn + ":";
    		}
    		
        	if(DEBUG_onScannedRobot || DEBUG_ALL) {
        		LOG[lineCount++] = "myHeading:" + myHeading + "\tmyPosX:" + myPosX + "\tmyPosY:" + myPosY + "\tmyEnergy:" + myEnergy;
        		LOG[lineCount++] = "enemyHeadingRelative:" + enemyHeadingRelative + "\tenemyVelocity:" + enemyVelocity;
        		LOG[lineCount++] = String.format("enemyBearingFromRadar:%.1f enemyBearingFromGun:%.1f enemyBearingFromHeading:%.1f", enemyBearingFromRadar, enemyBearingFromGun, enemyBearingFromHeading);
        		LOG[lineCount++] = "enemyDistance:" + enemyDistance + "\tenemyEnergy" + enemyEnergy;
        	}
    		
    		if(DEBUG_analysis || DEBUG_ALL) {
        		LOG[lineCount++] = "- analysis (weight vals)";
        		LOG[lineCount++] = "arr_wIH:" + Arrays.deepToString(arr_wIH);
        		LOG[lineCount++] = "arr_wHO:" + Arrays.deepToString(arr_wHO);
        	}
    		
    		obtainReward(reward_normalized);
            generatePrevs(y, Q_prev_new, prevStateActionVector, currentStateActionVector);
            generateCurrentStateVector(currentStateActionVector);
            RL_NN(currentStateActionVector, prevStateActionVector, y, Q_prev_new,
            		arr_wIH, arr_wHO,
            		wIH_past, wIH_next, wHO_past, wHO_next,
            		reward_normalized, activationMethod);
            resetReward(reward_normalized);
            doAction_Q(currentStateActionVector, enemyBearingFromHeading, enemyBearingFromGun);
    	}
        else {
        	doAction_notLearning(enemyBearingFromGun);
        }
    	doAction_mandatoryPerTurn(enemyBearingFromRadar);
    	
    }

    /**
     * @name:		boolean learnThisRound
     * @purpose:	Make a decision is RLNN should be run.
     * @param:		none, but uses int turn
     * @return:		boolean
     */
    public boolean learnThisRound() {
    	if (turn%learningRate == 0) {return true;}
    	else						{return false;} 
    }
    
	/**
     * @name:		obtainReward
     * @purpose:	calculates what the reward is for this cycle.
     * @param:		none
     * @return:		none
     */
    public void obtainReward(double reward_normalized){
    	double reward = 0.0;
    	energyDiffPrev = energyDiffCurr;
    	energyDiffCurr = myEnergy - enemyEnergy;
    	reward += energyDiffCurr - energyDiffPrev; //using +=, not =, to allow for other events to affect reward if desired.
    	reward_normalized = reward/600; //currently linear normalization. This fits the biggest possible enemy to around 0.05.
    	
    	if(DEBUG_obtainReward || DEBUG_ALL) {
    		LOG[lineCount++] = "- rewards";
    		LOG[lineCount++] = String.format("reward:%f reward(normalized):%.3f", reward, reward_normalized);
    	}
    }
    
    /**
     * @name:		generatePrevs
     * @purpose:	1. Copies Q_prev_new obtained from Q fxn in last round into Q_prev.
     * 				2. Copies last round's currSAV into prevSAV, to be used for NNet.
     * @param:		n, but uses:
     * 				1. Q_curr 
     * @return:		n
     */
    public void generatePrevs(double[] Q_prev, double[] Q_prev_new, double[] prevSAV, double[] currSAV){
    	if(DEBUG_generatePrevs || DEBUG_ALL) {
    		LOG[lineCount++] = "- generatePrevs:";
    		LOG[lineCount++] = "start values";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "currSAV: " + Arrays.toString(currSAV);
    		LOG[lineCount++] = "Q_prev: " + Arrays.toString(Q_prev);
    		LOG[lineCount++] = "Q_prev_new:" + Arrays.toString(Q_prev_new); 
    	}
    	
    	System.arraycopy(Q_prev_new, 0, Q_prev, 0, Q_prev.length);
    	System.arraycopy(currSAV, 0, prevSAV, 0, currSAV.length);
    	
    	if(DEBUG_generatePrevs || DEBUG_ALL) {
    		LOG[lineCount++] = "end values";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "currSAV: " + Arrays.toString(currSAV);
    		LOG[lineCount++] = "Q_prev: " + Arrays.toString(Q_prev);
    		LOG[lineCount++] = "Q_prev_new:" + Arrays.toString(Q_prev_new);
    		LOG[lineCount++] = "#eo generatePrevs";
    	}
    }
    
    /**
     * @name: 		generateCurrentStateVector
     * @brief:		Generates the current state vectors (NOT actions).
     * @purpose: 	1. gets state values from environment. 
     * 				2. Update array of current stateAction vector.  
     * @param: 		n
     * @return: 	none
     * currentStateVector positions [0][1][2] are all the actions. 
     */
    public void generateCurrentStateVector(double[] currentStateActionVector){
    	//First few INPUTS are ACTIONS and hence will be zeroed for generating currSAV
    	//INPUTS 0, 1 and 2 are ACTION
    	for (int i = 0; i < numActionContainers; i++) {
    		currentStateActionVector[i] = 0;
    	}
    	
    	//Dimension 3 - private static final int input_state0_myPos_possibilities = 5;
    	//left
    	if (  (myPosX<=50)  &&  ( (myPosX <= myPosY) || (myPosX <= (600-myPosY)) )  ){					
    		currentStateActionVector[3] = 1;						
    	}
    	
    	//right
    	else if (  (myPosX>=750)  &&  ( ((800-myPosX) <= myPosY) || ((800-myPosX) <= (600-myPosY)) )  ){
    		currentStateActionVector[3] = 2;						
    	}
    	
    	//top
    	else if (myPosY<=50) { 
    		currentStateActionVector[3] = 3;
    	}
    	
    	//bottom
    	else if (myPosY>=550) {				
    		currentStateActionVector[3] = 4;
    	}
    	
    	//center
    	else {
    		currentStateActionVector[3] = 0; 
    	}

    	//Dimension 4 - private static final int input_state1_myHeading_originalPossilibities = 4;
    	currentStateActionVector[4] = myHeading*4/360;			//to normalize. 
    	
    	//Dimension 5 - enemyEnergy
    	if (enemyEnergy < 30){
    		currentStateActionVector[5] = enemyEnergy/60;
    	}
    	
    	else if (enemyEnergy >= 30){
    		currentStateActionVector[5] =((enemyEnergy-30)/70)+0.5;
    	}
    	
    	//Dimension 6: enemyDistance  
    	//<150, <350, >=350(to1000)
    	currentStateActionVector[6] = enemyDistance;
    	if (enemyDistance < 150){
    		currentStateActionVector[6] = enemyDistance/100;
    	}
    	
    	else if (enemyDistance <= 350){
    		currentStateActionVector[6] =((enemyDistance-150)/200);
    	}
    	else if (enemyDistance > 350){
    		currentStateActionVector[6] = (enemyDistance/2000); 
    	}
    	
		//Dimension 7: is enemy moving right, left, or within the angle of my gun?
		//requires mygunheading, enemyheading, enemyvelocity
    	if ((enemyHeadingRelativeAbs < 30) || (enemyHeadingRelativeAbs > 150) || (enemyVelocity == 0)) {
			currentStateActionVector[7] = 0; //within angle of gun
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity > 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity < 0)) ) {
			currentStateActionVector[7] = 1; //enemy moving left
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity < 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity > 0)) ){
			currentStateActionVector[7] = 2; //enemy moving right
		}
    	
    	if(DEBUG_generateCurrentStateVector || DEBUG_ALL){
    		LOG[lineCount++] = "- generateCurrentStateVector:";
    		LOG[lineCount++] = "currentSAV:" + Arrays.toString(currentStateActionVector);
    		LOG[lineCount++] = "#eo generateCurrentStateVector";
    	}
    	
    }

    /**
     * @name:		RL_NN
     * @purpose: 	1. Cycle through all the possible actions via forward propagation with the current states, and calculate all Q values.
     * 				2. Find max Q value, determine action based on policy.
     * 				3. Perform Q function using the recorded previous Q value, and the just calculated the current Q value. Result is Q_prev_new
     * 				4. Readjust weights via backward propagation.
     * @param: 		many
     * @return: 	n
     */
    public void RL_NN(double[] currSAV, double[] prevSAV, double[] y, double[] Q_prev_new, //adjusted layer values
    					double[][] arr_wIH, double[][] arr_wHO, //weights
    					double[][] wIH_past, double[][] wIH_next, double[][] wHO_past, double[][] wHO_next, //backprop-momentum vars
    					double reward, boolean activationMethod){
    	
        double [][][] Q_NNFP_all = new double 				// list of generated Q values from currSAV-based FP
        		[input_action0_moveReferringToEnemy_possibilities]
        		[input_action1_fire_possibilities]
        		[input_action2_fireDirection_possibilities];
        double[] Q_curr = new double[numOutputsTotal];		// current cycle Q value generated from getMax
        double [] z_in = new double[numHiddensTotal]; 		// Array to store z[j] before being activate
    	double [] z    = new double[numHiddensTotal];		// Array to store values of z 
    	double [] y_in = new double[numOutputsTotal];		// Array to store Y[k] before being activated
    	//arrays in BP
    	double [][] vDelta = new double[numInputsTotal] [numHiddensTotal];	// Change in Input to Hidden weights
    	double [][] wDelta = new double[numHiddensTotal][numOutputsTotal]; 	// Change in Hidden to Output weights	  
    	double [] delta_out    = new double[numOutputsTotal];
    	double [] delta_hidden = new double[numHiddensTotal];
    	
    	
    	//TODO bookmark for main NN fxn.
    	getAllQsFromNet (Q_NNFP_all, currSAV, arr_wIH, arr_wHO, activationMethod);
        getMax			(Q_NNFP_all, currSAV, Q_curr, activationMethod); 
        qFunction		(Q_prev_new, y, reward, Q_curr);
        prepareBackProp	(prevSAV, z,
        					z_in, y_in,
        					arr_wIH, arr_wHO,
        					activationMethod);
        backProp		(prevSAV, z, y, Q_prev_new, 
        					z_in, y_in, 
        					delta_out, delta_hidden, vDelta, wDelta, 
        					arr_wIH, arr_wHO, 
        					activationMethod, 
        					wIH_past, wIH_next, wHO_past, wHO_next);
    }

    /** 
     * @name:		getAllQsFromNet
     * @input: 		currentStateVector 
     * @purpose: 	1. For current state, cycle through all possible actions and obtain all q-values (y), and stores in Q_NNFP_all.
     * 					With exception of the inputs and outputs, all NN structure parameters used are temporary parameters.
     * @param:		1. currSAV				aka currentStateActionVector or x, current state action vectors from environment.
     * 				2. Q_NNFP_all 			the Q values calculated from neural net forward propagation (aka y or output)
     * 				3. z					activated hidden layer
	 * 				4. Y					activated output layer, CURRENT (as opposed to past from Q fxn)
	 * 				5. z_in					pre-activation hidden layer
	 * 				6. y_in					pre-activation final layer
	 * 				7. arr_wIH				weights between input and hidden layer
	 * 				8. arr_wHO				weights between hidden and output layer
	 * 				9. activationMethod		binary (0 to 1) or bipolar (-1 to 1) activation function
     * @return: 	n
     */
	public void getAllQsFromNet(double [][][] Q_NNFP_all, double[] currSAV, double[][] arr_wIH, double[][] arr_wHO, boolean activationMethod) {
		
		double[] currSAV_temp = new double[numInputsTotal];
		double[] z_temp 	  = new double[numHiddensTotal];
		double[] y_temp 	  = new double[numOutputsTotal];
		double[] z_in_temp    = new double[numHiddensTotal];
		double[] y_in_temp    = new double[numOutputsTotal];

		if(DEBUG_getAllQsFromNet || DEBUG_MULTI_forwardProp || DEBUG_ALL){
			LOG[lineCount++] = "- getAllQsFromNet:";
			LOG[lineCount++] = "currSAV:" + Arrays.toString(currSAV);
    	}
		
		System.arraycopy(currSAV, 3, currSAV_temp, 3, numStateContainers);
		
		if(DEBUG_getAllQsFromNet || DEBUG_MULTI_forwardProp || DEBUG_ALL){
			LOG[lineCount++] = "currSAV_temp:" + Arrays.toString(currSAV_temp);
    	}
		
		for (int i_A0 = 0; i_A0 < input_action0_moveReferringToEnemy_possibilities; i_A0++){
			for (int i_A1 = 0; i_A1 < input_action1_fire_possibilities; i_A1++){
				for(int i_A2 = 0; i_A2 < input_action2_fireDirection_possibilities; i_A2++){
					currSAV_temp[0] = i_A0;
					currSAV_temp[1] = i_A1;
					currSAV_temp[2] = i_A2;
					forwardProp(currSAV_temp, z_temp, y_temp,
									z_in_temp, y_in_temp,
									arr_wIH, arr_wHO, 
									activationMethod);
					Q_NNFP_all[i_A0][i_A1][i_A2] = y_temp[0];
				}
			}
		}

    	if(DEBUG_getAllQsFromNet || DEBUG_MULTI_forwardProp || DEBUG_ALL){
    		LOG[lineCount++] = "Q_NNFP_all going into getMax:" + Arrays.deepToString(Q_NNFP_all);
    		LOG[lineCount++] = "#eo getAllQsFromNet";
    	}
    	
    	return;
	}
	
	/** 
	 * @name:		forwardProp
	 * @brief: forward propagation done in accordance to pg294 in Fundamentals of Neural Network, by Laurene Fausett.
	 * 			Feedforward (step 3 to 5):
	 * 				step 3: Each input unit (x[i], i = 1, ..., n) receives input signal xi and broadcasts this signal to all units in the layer above (the hidden units).
	 * 				step 4: Each hidden unit (z[j], j = 1, ..., p) sums its weighted input signals,
	 * 								z_in[j] = v[0][j] + (sum of from i = 1 to n)x[i]v[i][j],                <- v = weights between input and hidden.
	 * 						applies its activation fxn to compute its output signal,
	 * 								z[j] = f(z_in[j]),
	 * 						and sends this signal to all units in the layer above (output units).
	 * 				step 5: Each output unit (Y[k], k = 1, ..., m) sums its weighted input signals, (treating k = 0 to start instead of 1 for now b/c no output)
	 * 								y_in[k] = w[0][k] + (sum of from j = 1 to p)z[j]w[j][k]                 <- w = weights between hidden and output.
	 * 						and applies its activation fxn to compute its output signal,
	 * 								Y[k] = f(y_in[k])
	 * @purpose: does forwardPropagation on the inputs from the robot. 
	 * @param: 		can find the same(except Q_NNFP_all) from getAllQsFromNet(), which invokes this fxn.
	 * @param:		1. x					input layer
     * 				2. z					activated hidden layer
	 * 				3. y					ALLactivated output layer, CURRENT (in contrast to PAST used for Qfxn)
	 * 				4. z_in					pre-activation hidden layer, ie: sum of inputs*weights
	 * 				5. y_in					pre-activation final layer, ie: sum of hidden*weights
	 * 				6. arr_wIH				weights between input and hidden layer
	 * 				7. arr_wHO				weights between hidden and output layer
	 * 				8. activationMethod		binary (0 to 1) or bipolar (-1 to 1) activation function
	 * @return: n. 
	 **/
    public void forwardProp(double[] x, double[] z, double[] y,
    							double[] z_in, double[] y_in,
    							double[][] arr_wIH, double[][] arr_wHO,
    							boolean activationMethod) {
    	if(DEBUG_MULTI_forwardProp || DEBUG_forwardProp || DEBUG_ALL){
    		LOG[lineCount++] = "- FP:";
    		LOG[lineCount++] = "x:" + Arrays.toString(x);
    	}
    	
    	//step 3 and 4:    	
		for (int j = 1; j < numHiddensTotal; j++){ 		//p = numHiddensTotal
			double sumIn = 0.0;
			for (int i = 0; i < numInputsTotal; i++){	   //n = numInputsTotal
				sumIn += x[i]*arr_wIH[i][j]; //NO INPUT BIAS, that's why j = 1
			}
			z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			z_in[0] = valHiddenBias; 									//set z_in[0] = bias. HIDDEN BIAS = 1
			z[0] = z_in[0]; //can choose to optimize here by placing this outside of loop, since we know what valHiddenBias is.
			
			if (activationMethod == binaryMethod)
				z[j] = binaryActivation(z_in[j]); 				
			else
				z[j] = bipolarActivation(z_in[j]);
			
			if(DEBUG_MULTI_forwardProp || DEBUG_forwardProp || DEBUG_ALL){
				LOG[lineCount++] = String.format("z[%d]:%.16f z_in[%d]:%.3f sumIn%.3f", j, z[j], j, z_in[j], sumIn);
			}
			
		}
		//step 5:
		for (int k = 0; k < numOutputsTotal; k++){
			double sumOut = 0.0; 
			for (int j= 0; j < numHiddensTotal; j++){
				sumOut += z[j]*arr_wHO[j][k]; 
			}
			y_in[k] = sumOut; 	
			
			if (activationMethod == binaryMethod)
				y[k] = binaryActivation(y_in[k]); 
			else
				y[k] = bipolarActivation(y_in[k]);
			
			if(DEBUG_MULTI_forwardProp || DEBUG_forwardProp || DEBUG_ALL){
				LOG[lineCount++] = String.format("Y[%d]:%.16f y_in[%d]:%.3f sumOut%.3f", k, y[k], k, y_in[k], sumOut);
			}
			
		}
		return; 
	}
    
    /**
     * @name:		getMax()
     * @purpose: 	1. Obtain the action in current state with the highest q-value, 
     * 				   and its associated q-value. 
	 *					a. Start current max Q value at lower than obtainable value.
	 *					b. Cycle through all actions in current SAV, recording max q-values.
	 *						i. if indexQVal > QMax:
	 *							(1) Update QMax
	 *							(2) Set maxAction_totalNum = 1.
	 *							(3) Store the (now 3 dimension) action index into maxAction_all[maxAction_totalNum-1]
	 *						ii. if indexQVal == QMax:
	 *							(1) maxAction_totalNum++
	 *							(2) Store the (now 3 dimension) action index into maxAction_all[maxAction_totalNum-1]
	 *						iii. if indexQVal < QMax:
	 *							ignore.
	 *					c. record chosen action. If multiple actions with max q-values, randomize chosen action.
	 *						i. if maxAction_totalNum > 1, 
	 *						   randomly select between 0 and maxAction_totalNum - 1. The randomed 
	 *						   number will correspond to the array location of the chosen
	 *						   action in maxAction_all. 
	 *						ii. maxAction_policyBasedSelection = maxAction_all[randomed number]
	 *					d. record associated q-value.
     * @param: 		1.	Q_NNFP_all		array of q values for the forward propagations
     * 				2.	current SAV[]	current state action vectors
     * 				3.  Q_curr[0]		stores the maximum Q value //Joey: assuming there's only one possible max, for now (one output)
     * 				4.	activationMethod	binary/bipolar layer value normalization
     * @return: 	n
     */
    public void getMax(double[][][] Q_NNFP_all, double[] currSAV, double[] Q_curr, boolean activationMethod) {
    	//QMax stores the maximum Q found. starting at -100 allows the first one to be picked even if it's super negative. //Joey: apparently the reward system is so fucked that -1E99 can happen so, bug (May 31) don't we normalize Q val?
    	double QMax = -100.0;  
    	//total number of actions with the same value as the max Q.
        int maxAction_totalNum = 0;
        //used to generate a random number starting from 0 to maxAction_totalNum.
        int maxAction_arrIndex = 0;
        //this var stores the multi-dimensional actions into one container instead of multiple containers. Downstream functions require a linear action dimension.
        int forLoopsLinearized = 0;
        //stores the chosen action with maximum Q.
        int maxAction_policyBasedSelection = 0;
        //array for storing all actions with maxqval
        int [] maxAction_all = new int [numActions];
        //randomizes an action number. used for different policies.
        int randomVal = 0;
        
        
    	if(DEBUG_MULTI_forwardProp || DEBUG_getMax || DEBUG_ALL) {
        	LOG[lineCount++] = "Q_NNFP_all:                  " + Arrays.deepToString(Q_NNFP_all);
        }
    	
    	// calculates all max values and stores multiple (really rare)
    	for (int i_A0 = 0; i_A0 < Q_NNFP_all.length; i_A0++){
		    for (int i_A1 = 0; i_A1 < Q_NNFP_all[0].length; i_A1++){
		    	for (int i_A2 = 0; i_A2 < Q_NNFP_all[0][0].length; i_A2++, forLoopsLinearized++){
		    		if (Q_NNFP_all[i_A0][i_A1][i_A2] > QMax){
		    			QMax = Q_NNFP_all[i_A0][i_A1][i_A2];
		            	maxAction_totalNum = 1;
		            	maxAction_all[maxAction_totalNum-1] = forLoopsLinearized;		
		            }
		            else if (Q_NNFP_all[i_A0][i_A1][i_A2] == QMax){
		            	maxAction_all[maxAction_totalNum++] = forLoopsLinearized;
		            }	            
		    	}
    		}
    	}
    	
    	//max Q value found
        Q_curr[0] = QMax;
        
    	if(DEBUG_MULTI_forwardProp || DEBUG_getMax || DEBUG_ALL) {
        	LOG[lineCount++] = "maxAction_all:" + Arrays.toString(maxAction_all);
        	LOG[lineCount++] = "maxAction_totalNum: " + maxAction_totalNum;
        }
        
        if (maxAction_totalNum > 1) {
        	maxAction_arrIndex = (int)(Math.random()*(maxAction_totalNum)); //math.random randoms btwn 0.0 and 0.999. Allows selection array position from 0 to num-1 through int truncation. 
        	
        	if(DEBUG_MULTI_forwardProp || DEBUG_getMax || DEBUG_ALL) {
            	LOG[lineCount++] = ">1 max vals, randomly chosen action " + maxAction_arrIndex;
            }
        }
        
        //Choosing next action based on policy. Greedy is default
        //exploratory uses this line to perform if-false actions.
        maxAction_policyBasedSelection = maxAction_all[maxAction_arrIndex]; //if maxAction_totalNum <= 1, maxAction_arrIndex = 0;
        
        
        //note: sarsa is currently not used. explained slightly further in comments in global final section near top of file.
        if (policy == SARSA || policy == exploratory) {
	    	randomVal = (int)(Math.random()*(numActions));
	        if (policy == SARSA) {
	        	maxAction_policyBasedSelection = randomVal;
	        }
	        else if(policy == exploratory) {
	        	maxAction_policyBasedSelection = (Math.random() > epsilon ? maxAction_policyBasedSelection : randomVal);
	        }
        }
	        
        if(DEBUG_MULTI_forwardProp || DEBUG_getMax || DEBUG_ALL) {
        	LOG[lineCount++] = "enacting policy:" + policy + "(0=gre 1=exp 2=SAR)";
        	LOG[lineCount++] = String.format("Action Chosen (linear) %d", maxAction_policyBasedSelection);
        	LOG[lineCount++] = "lengths:" + Q_NNFP_all.length + Q_NNFP_all[0].length + Q_NNFP_all[0][0].length;
        }
        
        OUTERMOST: for (int i_A0 = 0; i_A0 < input_action0_moveReferringToEnemy_possibilities; i_A0++){
			for (int i_A1 = 0; i_A1 < input_action1_fire_possibilities; i_A1++){
				for(int i_A2 = 0; i_A2 < input_action2_fireDirection_possibilities; i_A2++){
		    		if (maxAction_policyBasedSelection < 1) {
		    			//currSAV glob var updated here
		    			currSAV[0] = i_A0; 
		    			currSAV[1] = i_A1;
		    			currSAV[2] = i_A2;
		    			
		    			break OUTERMOST;
		    		}
		    		maxAction_policyBasedSelection--;
		    	}
		    }
        }
        
		if(DEBUG_MULTI_forwardProp || DEBUG_getMax || DEBUG_ALL) {
        	LOG[lineCount++] = "chosen actions(in containers):" + (int)currSAV[0] + " " + (int)currSAV[1] + " " + (int)currSAV[2];
        	LOG[lineCount++] = "with output: " + Q_curr[0];
        	LOG[lineCount++] = "#eo muxFP";
        }

        return;
    }
    
    /**
     * @name		qFunction
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		1. Q_prev_new		aka Q_target, t. Records the corrected Qval.
     * 				2. Q_prev			aka y. The old corrected Qval.
     * 				3. reward			reward value - needs work. //joey: XD
     * 				4. Q_curr			Q value calculated during FP for current round. Used to correct Qval depending on its weight (gamma).
     * 				Utilizes following critical global vars directly:
     * 				1. gamma			describes weight of current Q value in calculation.
     * 				2. alpha			describes the extent to which the newly acquired information will override the old information.
     * @return		prevQVal
     */
    public void qFunction(double[] Q_prev_new, double[] Q_prev, double reward, double[] Q_curr){ //Joey: consider changing Q_prev into entire array.
    	
    	//Joey: ask andrea about papers for good gamma terms. (close to 1?)
    	
		Q_prev_new[0] = Q_prev[0] + alpha*(reward + (gamma*Q_curr[0]) - Q_prev[0]);
    	
    	//for debugging purposes: file recording Qval fluctuation
    	if (flag_recordQVals) {
    		arr_QVals[totalQValRecords++] = Q_curr[0];
    	}
    	if(DEBUG_qFunction || DEBUG_ALL) {
    		LOG[lineCount++] = "- qFunction:";
    		LOG[lineCount++] = String.format("Q_prev_new(t)%.3f  Q_prev(y):%.3f  Q_curr:%.3f", Q_prev_new[0], Q_prev[0], Q_curr[0]);
    		LOG[lineCount++] = String.format("alpha:%.2f reward:%.3f gamma:%.2f", alpha, reward, gamma);
    		LOG[lineCount++] = "#eo qFunction";
    	}
    }
 
    /** 
     * @name:		prepareBackProp
     * @purpose:	Populate NN parameters using previous SAV, in order to perform back propagation.
     * @param:		1. prevSAV		input to generate net - inputs
     * 				2. z			refreshes prev hidden layer
     * 				3. z_in			refreshes prev raw hidden layer
     * 				4. y_in			refreshes prev raw output layer
     * 				5. arr_wIH		input to generate net - IH weights
     * 				6. arr_wHO		input to generate net - HO weights
     * 				7. activationMethod 	binary/bipolar method of normalizing layers
     */
    public void prepareBackProp (double[] prevSAV, double[] z,
    								double[] z_in, double[] y_in,
    								double[][] arr_wIH, double[][] arr_wHO,
    								boolean activationMethod) {
    	
    	double[] y_temp = new double [numOutputsTotal];
    	
    	if(DEBUG_MULTI_backProp || DEBUG_prepareBackProp || DEBUG_ALL) {
    		LOG[lineCount++] = "- prepareBackProp:";
    		LOG[lineCount++] = "start list";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "z: " + Arrays.toString(z);
    		LOG[lineCount++] = "z_in: " + Arrays.toString(z_in);
    		LOG[lineCount++] = "y_in:" + Arrays.toString(y_in);
    	}
    	
    	forwardProp(prevSAV, z, y_temp,
    				z_in, y_in,
    				arr_wIH, arr_wHO,
    				activationMethod);
    	
    	if(DEBUG_MULTI_backProp || DEBUG_prepareBackProp || DEBUG_ALL) {
    		LOG[lineCount++] = "after list";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "z: " + Arrays.toString(z);
    		LOG[lineCount++] = "z_in: " + Arrays.toString(z_in);
    		LOG[lineCount++] = "y_in:" + Arrays.toString(y_in);
    		LOG[lineCount++] = "y_temp:" + Arrays.toString(y_temp); 
    		LOG[lineCount++] = "#eo prepareBackProp";
    	}
    }
    
    /**
     * @name:		backProp
     * @purpose:	Adjusts weights based on the difference between Q_prev_new and Q_prev.
     * @methodology:pg 295 in Fundamentals of Neural Networks by Lauren Fausett, Backpropagation of error: steps 6 to 8.
     * 				step 6:
     * 				Each output unit (Y[k], k = 1, ..., m) receives a target pattern corresponding to the input training pattern, computes its error information term,
     * 					delta_out[k] = (t[k] - y[k])f'(y_in[k]),
     * 				calculates its weight correction term (used to update w[j][k] later),
     * 					delta_weight_w[j][k] = alpha * delta[k] * z[j],
     * 				calculates its bias correction term (used to update w[0][k] later),
     * 					delta_weight_w[0][k] = alpha * delta[k],
     * 				and continue to use delta[k] for lower levels.
     *				
     *				step 7: 
     *				Each hidden unit (z[j], j = 1 ..., p) sums its delta inputs (from units in the layer above),
     *					delta_in[j] = (sum of from k = 1 to m)(delta[k] * w[j][k]),
     *				multiplies by the derivative of its activation fxn to calculate its error information term,
     *					delta[j] = delta_in[j] * f'(z_in[j]),
     *				calculates its weight correction term (used to update v[i][j] later),
     *					delta_weight_v[i][j] = alpha * delta[j] * x[i],
     *				and calculates its bias correction term (used to update v[0][j] later),
     *					delta_weight_v[0][j] = alpha * delta[j].
     *				
     *				step 8: Update weights and biases
     *				Each output unit (Y[k], k = 1, ..., m) updates its bias and weights (j = 0, ..., p):
     *					w[j][k](new) = w[j][k](old) + delta_weights_w[j][k].
     *				Each hidden unit (z[j], j = 1, ..., p) updates its bias and weights (i = 0, ..., n):
     *					v[i][j](new) = v[i][j](old) + delta_weights_v[i][j].
     *
     *				To assist with rate of convergence, we have also included the ability for the net to use momentum. Momentum requires data from one or more previous
     *				training patterns. In the simplest form, the weights at t+1 are based on the weights at t and t-1:
     *					w[j][k](t+1) = w[j][k](t) + alpha*delta_out[k]*z[j] + mu[w[j][k](t) - w[j][k](t-1)],
     *				and
     *					v[i][j](t+1) = v[i][j](t) + alpha*delta_in[j]*x[i] + mu[v[i][j](t) - v[j][k](t-1)].
     * @param:		BP variables:
     * 					1. x <- prevSAV
     * 					2. z <- previous cycle's hidden layer
     * 					3. y <- Q_prev: array of previous Q value from previous cycle.
     * 					4. t <- Q_prev_new: array of current calculated Q value from Q function.
     * 				Other general vars:
     * 					1. activationMethod (not global to reserve possibility of changing its value)
     * 				Momentum variables, which remembers past values:
     * 					1. vPast <- wIH_past 
     * 					2. vNext <- wIH_next
     * 					3. wPast <- wHO_past
     * 					4. wNext <- wHO_next
     * 					
     * @return:		n
     */
    public void backProp(double[] x, double[] z, double[] y, double[] t,
    						double[] z_in, double[] y_in, 
    						double[] delta_out, double[] delta_hidden, double[][] vDelta, double[][] wDelta, 
    						double[][] arr_wIH, double[][] arr_wHO, 
    						boolean activationMethod, 
    						double [][] vPast, double [][] vNext, double [][] wPast, double [][] wNext) {      
    	
    	//local var used to store activation derivative of y.
    	double[] temp_outputDerivative = new double [numOutputsTotal];
    	//local var stores raw output error - for debugging purposes.
    	double temp_outputErrorRaw = 0;
    	
    	if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
			LOG[lineCount++] = "- BP";
			LOG[lineCount++] = "momentum:" + momentum;
		}
    	//Y_target is the variable calculated in QFunction to depict NN's converging(hopefully) approximation of the RL LUT.
 
        
    	//step 6-8 for hidden-to-output weights
        if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
			LOG[lineCount++] = "@output cycle:";
			LOG[lineCount++] = "arr_wHO(pre):" + Arrays.deepToString(arr_wHO);
		}
        //step 6:
		for (int k = 0; k <numOutputsTotal; k++){ // m = numOutputsTotal. pretending output bias doesn't exist so our output vector starts at 0 (horrificallylazyXD)
			
			//delta_out[k] = (t[k] - y[k])f'(y_in[k])
			temp_outputErrorRaw = t[k] - y[k];
			
			if (activationMethod == binaryMethod){
				temp_outputDerivative[k] = binaryDerivative(y_in[k]);
				delta_out[k] = temp_outputErrorRaw*temp_outputDerivative[k]; 
			}
			else{
				temp_outputDerivative[k] = bipolarDerivative(y_in[k]);
				delta_out[k] = temp_outputErrorRaw*temp_outputDerivative[k];	
			}
			
			//misc data collections: calculating back propagation error for convergence calculation.
			if(flag_recordBPErrors) {
	        	arr_BPErrors[totalBPErrorsRecords++] = temp_outputErrorRaw; //thankfully, currently one output. Will need to correct code if more than error.
	        }
			
			if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
				LOG[lineCount++] = String.format("delta_out[%d]:%.3f error_raw:%.8f (%s)", k, delta_out[k], temp_outputErrorRaw, (activationMethod==binaryMethod)?"bin":"bip");
				LOG[lineCount++] = String.format("t(target)[%d]:%.3f y(calc'd)[%d]:%.3f y_in[%d]:%.3f y_in_der[%d]:%.3f", k, t[k], k, y[k], k, y_in[k], k, temp_outputDerivative[k]);
			}
			
			//delta_weight_w[j][k] = alpha * delta[k] * z[j]
			for (int j = 0; j < numHiddensTotal; j++){
				wDelta[j][k] = alpha*delta_out[k]*z[j];
				
				if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
					LOG[lineCount++] = String.format("wDelta[%d][%d]:%.3f wNext[%d][%d]:%.3f wPast[%d][%d]:%.3f", j, k, wDelta[j][k], j, k, wNext[j][k], j, k, wPast[j][k]);
				}
				
				//step 8: updating H-O weights using momentum
				wNext[j][k] = arr_wHO[j][k] + wDelta[j][k] + momentum*(arr_wHO[j][k] - wPast[j][k]); 
				wPast[j][k] = arr_wHO[j][k]; 
				arr_wHO[j][k] = wNext[j][k]; 
			}
		}
		
		if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
			LOG[lineCount++] = "arr_wHO(post):" + Arrays.deepToString(arr_wHO);
		}
		
		//step 7:
		//for input-to-hidden layer
		
        if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) { 
        	LOG[lineCount++] = "@i-to-h cycle:";
			LOG[lineCount++] = "arr_wIH(pre):" + Arrays.deepToString(arr_wIH);
		}
        
		for (int j = 0; j < numHiddensTotal; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutputsTotal; k++){ //pretending output bias doesn't exist so our output vector starts at 0, when it should start at 1 if a slot is reserved for bias
				sumDeltaInputs += delta_out[k]*arr_wHO[j][k];
				if (activationMethod == binaryMethod){
					delta_hidden[j] = sumDeltaInputs*binaryDerivative(z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(z_in[j]);	
				}
			}
			for (int i = 0; i< numInputsTotal; i++){ //because no input bias, i = 0 will be a wasted cycle (ah wellz)
				vDelta[i][j] = alpha*delta_hidden[j]*x[i];
				
				if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
					LOG[lineCount++] = String.format("vDelta[%d][%d]:%.3f vNext[%d][%d]:%.3f vPast[%d][%d]:%.3f", i, j, vDelta[i][j], i, j, vNext[i][j], i, j, vPast[i][j]);
				}
				
				//step 8: updating I-H weights using momentum
				vNext[i][j] = arr_wIH[i][j] + vDelta[i][j] + momentum*(arr_wIH[i][j] - vPast[i][j]); //Joey: rest of this
				vPast[i][j] = arr_wIH[i][j]; 
				arr_wIH[i][j] = vNext[i][j]; 
			}
		}
		
        if(DEBUG_MULTI_backProp || DEBUG_backProp || DEBUG_ALL) {
			LOG[lineCount++] = "arr_wIH(post):" + Arrays.deepToString(arr_wIH);
		}
        
//		
//		//Step 9 - Calculate local error. For debugging purposes; an additional way to measure if QVals is converging. //Joey: add flag to decide whether this is used.
//		double error = 0.0;
//		for (int k = 0; k < numOutputsTotal; k++){ 
//			error = 0.5*(java.lang.Math.pow((Y_target[k] - Y_calculated[k]), 2)); 
//		}
	}
	
    /**
     * @name:		resetReward
     * @purpose: 	Resets reward to 0.
     * @param: 		1. reward
     * @return:		n
     */
    public void resetReward(double reward){
        
        reward = 0;
        
    }
    
    /**
     * @name:		doAction_Q
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0], and other analysis specific actions.
     * @param: 		1. Array currentSAV.
     * 				2. enemyBearingFromHeading:		enemy's bearing in terms of degrees from our bot's heading
     * 				3. enemyBearingFromGun:			enemy's bearing in terms of degrees from the direction which our gun is pointing to
     * @return:		n
     */
    public void doAction_Q(double[] currSAV, double enemyBearingFromHeading, double enemyBearingFromGun){
    	//maneuver behaviour (chase-offensive/defensive)
    	if      ( currSAV[0] == 0 ) {setTurnRight(enemyBearingFromHeading); 									setAhead(50); }
    	else if ( currSAV[0] == 1 ) {setTurnRight(enemyBearingFromHeading); 									setAhead(-50);}
    	else if ( currSAV[0] == 2 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90)); 	setAhead(50); }
    	else if ( currSAV[0] == 3 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading + 90)); 	setAhead(50); }
    	
    	if      ( currSAV[1] == 0 ) {setFire(1);}
    	else if ( currSAV[1] == 1 ) {setFire(3);}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if      ( currSAV[2] == 0 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));}
    	else if ( currSAV[2] == 1 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 10));}
    	else if ( currSAV[2] == 2 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 10));}   	
    	
    	if(DEBUG_doAction_Q || DEBUG_ALL) {
    		LOG[lineCount++] = "- doAction_Q:";
    		LOG[lineCount++] = "currSAV (chosen actions):" + Arrays.toString(currSAV);
    		LOG[lineCount++] = "#eo doAction_Q.";
    	}
    }

    /**
     * @name:		doAction_notLearning
     * @purpose: 	performs actions for rounds that do not perform learning, mainly to maintain gun angle proximity to enemy.
     * @param: 		1. enemyBearingFromGun:			enemy's bearing in terms of degrees from the direction which our gun is pointing to
     * @return:		n
     */
    public void doAction_notLearning(double enemyBearingFromGun) {
    	setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    }

    /**
     * @name:		doAction_mandatoryPerTurn
     * @purpose: 	performs actions mandatory for the round, mostly to maintain radar lock on the enemy.
     * @param: 		1. enemyBearingFromRadar		enemy's bearing in terms of degrees from the direction which the bot radar is pointing to
     * @return:		n
     */
    public void doAction_mandatoryPerTurn(double enemyBearingFromRadar) {
	    setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
	    scan();
	    execute();
    }
    
    /**
     * @name:		importDataWeights
     * @author:		Andrea; partly written in sittingduckbot
     * @purpose:	to extract neural net weights stored in finalHiddenWeights.txt 
     * 				and finalOuterWeights.txt into arrays NNWeights_inputToHidden[][]
     * 				and NNWeights_hiddenToOutput[][], respectively.
     * @param:		n, but uses these globals:
     * 				NNWeights_inputToHidden[][]
     * 				NNWeights_hiddenToOutput[][]
     * @return:		n
     */
    public int importDataWeights() {
    	if (flag_weightsImported == false) {
	    	try {
	        	BufferedReader reader = null;
	        	
	            try {
	            	if (flag_useOfflineTraining) {reader = new BufferedReader(new FileReader(getDataFile("inToHiddenWeights_OfflineTraining.txt")));}
	            	else                         {reader = new BufferedReader(new FileReader(getDataFile("finalHiddenWeights.txt"))); }
	            	
	            	for (int i = 0; i < numInputsTotal; i++) {
	            		for (int j = 0; j < numHiddensTotal; j++) {
	            			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
		                }
	            	}
	            } 
	            finally {
	                if (reader != null) {
	                    reader.close();
	                }
	            }
	            
	            BufferedReader reader2 = null;
	            try {
	            	if (flag_useOfflineTraining) {reader2 = new BufferedReader(new FileReader(getDataFile("hiddenToOutWeights_OfflineTraining.txt")));}
	            	else                         {reader2 = new BufferedReader(new FileReader(getDataFile("finalOuterWeights.txt")));}
	            	
	            	for (int i = 0; i < numHiddensTotal; i++) {
	            		for (int j = 0; j < numOutputsTotal; j++) {
	            			arr_wHO[i][j] = Double.parseDouble(reader2.readLine());
		                }
	            	}
	            } 
	            finally {
	                if (reader2 != null) {
	                    reader2.close();
	                }
	            }
	        } 
	        //exception to catch when file is unreadable
	        catch (IOException e) {
	            return ERROR_12_importWeights_IOException;
	        } 
	        // type of exception where there is a wrong number format (type is wrong or blank)  
	        catch (NumberFormatException e) {
	            return ERROR_13_importWeights_typeConversionOrBlank;
	        }
	    	
	    	flag_weightsImported = true;
	    	if (flag_useOfflineTraining) {
	    		flag_useOfflineTraining = false;
	    	}
	    	return SUCCESS_importDataWeights;
    	}
    	
    	else {
    		return ERROR_17_importWeights_flagImportedFalse;
    	}
    }
    
    /**
     * @name: 		exportDataWeight
     * @author: 	Andrea; mostly sittingduckbot
     * @purpose: 	1. stores the weights back into finalHiddenWeights.txt, 
     * 				finalOuterWeights.txt from data NNWeights_inputToHidden[][]
     * 				and NNWeights_hiddenToOutput[][], respectively.
     * 
     */
    public int exportDataWeights() {
    	if(flag_weightsImported == true) {
			PrintStream w1 = null;
	    	try {
	    		w1 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalHiddenWeights.txt")));
	    		if (w1.checkError()) {
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Something done messed up (Error 14 cannot write)";
	            	}
	            	return ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden;
	    		}
	    		 
	    		for (int i = 0; i < numInputsTotal; i++) {
	         		for (int j = 0; j < numHiddensTotal; j++) {
	         			w1.println(arr_wIH[i][j]);
	                }
	         	} 
	    	}
	    	catch (IOException e) {
	    		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out); 
	            return ERROR_16_exportWeights_IOException;
	        } 
	        finally {
	            if (w1 != null) {
	                w1.close();
	            }
	        }      
	    	PrintStream w2 = null;
	    	try {
	    		
	    		w2 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalOuterWeights.txt")));
	    		if (w2.checkError()) {
	                //Error 0x03: cannot write
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Something done messed up (Error 15 cannot write)";
	            	}
	            	return ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput;
	    		 }
	    		 
	    		for (int i = 0; i < numHiddensTotal; i++) {
	         		for (int j = 0; j < numOutputsTotal; j++) {
	         			w2.println(arr_wHO[i][j]);
	                }
	         	}
	    	}
	    	catch (IOException e) {
	    		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out);
	            return ERROR_16_exportWeights_IOException;
	        } 
	        finally {
	            if (w2 != null) {
	                w2.close();
	            }
	        }    
	    	
	    	
	    	flag_weightsImported = false;
	    	return SUCCESS_exportDataWeights;
    	}
    	else {
    		return ERROR_18_exportWeights_flagImportedTrue;
    	}
    }
    
    //TODO marker for import/export
    /**
     * @name:		importData
     * @author:		partly written in sittingduckbot
     * @purpose: 	1. Imports data from file, depending on file name. 
     * @brief:		ONLY A SINGLE FILE CAN BE IMPORTED AT A TIME.
     * 				Class BufferReader(java.io.) is instantiated to read file with filename using 
     * 				code obtained mainly from SittingDuck.java. The first line of the .dat file is
     * 				extracted to obtain the file settings, which includes the contents the file
     * 				expects. A set of slightly varying extraction sequences are performed 
     * 				based on file.
     * 				
     * 				Most available config settings:
     * 				    stringTest: 16400 (0x4010)
     * 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
     *    				WL:			16448 (0x4040), zero WL = 16449 (0x4041)
     *					NN weights: 16512 (0x4080), zeroing = 16513 (0x4081)
     *					QVals:		16640 (0x4100), zeroing = 16641 (0x4101)
     *					BPErrors:	16896 (0x4200), zeroing = 16897 (0x4201)
     * 				
     * @param: 		1. stringname of file desired to be written. The fxn currently accepts 3(three) 
     * 				files: LUTTrackfire.dat, winlose.dat, and stringTest.dat. Any other string 
     * 				name used for file name will be flagged as erroneous.
     * 				also uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports by multiple instances of robot (hopefully?).
     * @return:		1. int importLUTDATA success/error;
     */
    public int importData(String strName){
    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
    		LOG[lineCount++] = "- importData: at beginning of fxn";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
    	}
    	
        try {
        	BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(getDataFile(strName)));
                
                //reads first line of code to obtain what is the file. Information about file must be available in the first line of the file for it to be used by prog.
                fileSettings_temp = (short)Integer.parseInt(reader.readLine());			
                
                if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
                	LOG[lineCount++] = "extracted fileSettings into default: ";
                	LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
            	}
                
                // CONFIGMASK_VERIFYSETTINGSAVAIL = 0x4000
                // & is bit-wise "and". It compares each bit of the chosen CONFIGMASK with fileSettings_temp.
                // CONFIGMASK_VERIFYSETTINGSAVAIL is used to make sure the value in the first line is a fileSettings number (0x4000 is too large to be used as weights or LUT HOPEFULLY T_T)
                if ((fileSettings_temp & CONFIGMASK_VERIFYSETTINGSAVAIL) != CONFIGMASK_VERIFYSETTINGSAVAIL) {
                	return ERROR_3_import_verification;
                }
                
                //else: we verified the file has a fileSettings line. Let's read what file it is!
                //this prevents accidentally importing from wrong file. It matches the filename given to the function with the fileSettings read from the file.
                else { 
                	
                	if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_stringTest) == CONFIGMASK_FILETYPE_stringTest) && (flag_stringTestImported == false) ) {
                		if (strName != "stringTest.dat") {
                			return ERROR_4_import_wrongFileName_stringTest;
                		}
                		//else: the fileSettings read from the file matches with the one given to the function, continue!
                		//Clarification: the reason behind having both flag and file-specific fileSettings variable set, is due to the fact that the program
                		//	may change the fileSettings (eg. zeroing file on purpose) by writing a different settings back, so there must be a way to store the settings.
                		fileSettings_stringTest = fileSettings_temp; 
                		flag_stringTestImported = true;
                	}

                	else if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_weights) == CONFIGMASK_FILETYPE_weights) && (flag_weightsImported == false) ) {
                		if (strName != "weights.dat") {
                			return ERROR_19_import_wrongFileName_weights;
                		}
            			if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- writing blank weights into local weights array: ";
                    		}
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_wIH[i][j] = 0;
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_wHO[i][j] = 0;
        		                }
        	            	}
            				//Subtracts zeroingfile setting from fileSettings, so that the weights are zeroed only once.
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank weights.";
                    		}
            				
            			}
            			else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- writing recorded weights into local weights array: ";
                    		}
            				
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_wHO[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				//value 999 is at the end of the weights file to make sure net is the desired size.
            				//TODO learn interaction with EOF
            	            if (Double.parseDouble(reader.readLine()) != 999) {
            	            	return ERROR_20_import_weights_wrongNetSize;
            	            }
            	            
            	            if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            	            	LOG[lineCount++] = "Imported recorded weights.";
                    		}
            			}
            			fileSettings_weights = fileSettings_temp;
            			flag_weightsImported = true;
            		}
                	
                	
                	//WL Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_winLose) == CONFIGMASK_FILETYPE_winLose) && (flag_WLImported == false) ) {
                		if (strName != "winlose.dat") {
                			return ERROR_5_import_wrongFileName_WL; //error 5 - coder mislabel during coding
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking fight records (winLose):";
                    		}
            				totalFights = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				for (int i = 0; i < battleResults.length; i++){
	                    			battleResults[i] = 0;
	                    	}
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- importing saved fight records (winLose):";
                    		}
	                		totalFights = Integer.parseInt(reader.readLine());
	                    	for (int i = 0; i < battleResults.length; i++){
	                    		if (i < totalFights) {
	                    			battleResults[i] = Integer.parseInt(reader.readLine());
	                    		}
	                    		else {
	                    			battleResults[i] = 0;
	                    		}
	                    	}
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved fight records.";
                    		}
                		}
                    	fileSettings_WL = fileSettings_temp;
                    	flag_WLImported = true;
                	} // end of WinLose
                	
                	//QVals Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_QVals) == CONFIGMASK_FILETYPE_QVals) && (flag_QValsImported == false) ) {
                		if (strName != "qVals.txt") {
                			return ERROR_21_import_wrongFileName_QVals; //error 21 - coder mislabel during coding
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {                			
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking QVal records:";
                    		}
            				
            				totalQValRecords = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				
            				for (int i = 0; i < arr_QVals.length; i++){
	                    			arr_QVals[i] = 0;
	                    	}
            				
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank QVal records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- importing saved QVals:";
                    		}
	                		
            				totalQValRecords = Integer.parseInt(reader.readLine());
            				for (int i = 0; i < arr_QVals.length; i++){
	                    		if (i < totalQValRecords) {
	                    			arr_QVals[i] = Double.parseDouble(reader.readLine());
	                    		}
	                    		else {
	                    			arr_QVals[i] = 0;
	                    		}
	                    	}
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved QVals.";
                    		}
                		}
                    	fileSettings_QVals = fileSettings_temp;
                    	flag_QValsImported = true;
                	}//eo QVals
                	
                	//BPErrors Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_BPErrors) == CONFIGMASK_FILETYPE_BPErrors) && (flag_BPErrorsImported == false) ) {
                		if (strName != "BPErrors.txt") {
                			return ERROR_22_import_wrongFileName_BPErrors; //error 22: file mislabel (wuht how did i screw this up)
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {                			
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking BPErrors records:";
                    		}
            				
            				totalBPErrorsRecords = 0;
            				
            				for (int i = 0; i < arr_BPErrors.length; i++){
	                    			arr_BPErrors[i] = 0;
	                    	}
            				
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank BPErrors records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- importing saved BPErrors:";
                    		}
	                		
            				totalBPErrorsRecords = Integer.parseInt(reader.readLine());
            				for (int i = 0; i < arr_BPErrors.length; i++){
	                    		if (i < totalBPErrorsRecords) {
	                    			arr_BPErrors[i] = Double.parseDouble(reader.readLine());
	                    		}
	                    		else {
	                    			arr_BPErrors[i] = 0;
	                    		}
	                    	}
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved BPErrors.";
                    		}
                		}
                    	fileSettings_BPErrors = fileSettings_temp;
                    	flag_BPErrorsImported = true;
                	}//eo BPErrors
                	
                	//write code for new file uses here. 
                	//also change the string being called 
                	//ctr+f: Import data. ->Change imported filename here<- 
                	
                	//file is undefined - so returns error 8
                	else {
                		if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
                    		LOG[lineCount++] = "error 8:";
                    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
                    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//                    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
                    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
                    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
                    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
                    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
//                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_LUTTrackfire|verification: " + (CONFIGMASK_FILETYPE_LUTTrackfire | CONFIGMASK_VERIFYSETTINGSAVAIL);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_winLose|verification: " + (CONFIGMASK_FILETYPE_winLose | CONFIGMASK_VERIFYSETTINGSAVAIL);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_weights|verification: " + (CONFIGMASK_FILETYPE_weights | CONFIGMASK_VERIFYSETTINGSAVAIL);
//                    		LOG[lineCount++] = "flag_LUTImported: " + flag_LUTImported;
                    		LOG[lineCount++] = "flag_weightsImported: " + flag_weightsImported;
                    		LOG[lineCount++] = "fileSettings_temp & CONFIGMASK_ZEROINGFILE: " + (fileSettings_temp & CONFIGMASK_ZEROINGFILE);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_weights: " + CONFIGMASK_FILETYPE_weights;
                    		
                    	}
                		return ERROR_8_import_dump; //error 8 - missed settings/file dump.
                	}
                }
            } 
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } 
        //exception to catch when file is unreadable
        catch (IOException e) {
        	//error in file reading
            return ERROR_1_import_IOException;
        } 
        // type of exception where there is a wrong number format (type is wrong or blank)  
        catch (NumberFormatException e) {
        	//Error0x02 error in type conversion - check class throw for more details
            return ERROR_2_import_typeConversionOrBlank;
        }
       
    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
    		LOG[lineCount++] = "end of fxn fileSettings check (succeeded):";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
    	}
        return SUCCESS_importData;
    }
    
    /**
     * @name: 		exportData()
     * @author: 	partially written in robocode's sittingduckbot
     * @purpose:	exports stored file data into strName.
     * @brief:		Export is done once per file.
     * 				1. the fxn contains multiple if-scopes, each for a file.
     * 				the first checks (stringName matches scope's target) 
     * 								 && (fileSettings_target is set) 
     *                               && (flag_preventMultipleFile is true)
     *              2. write into file
     *              	a. write fileSettings_target
     *              	b. write data
     *              3. flip flag_preventMultipleFile
     *              
     * 				Most available config settings:
     * 				    stringTest: 16400 (0x4010)
     * 					strLUT:		16416 (0x4020)
     *    				WL:			16448 (0x4040)
     *					NN weights: 16512 (0x4080)
     *					QVals:		16640 (0x4100)
     *					BPErrors:	16896 (0x4200)
     * 
     * @param: 		1. string of file name
     * 				and uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports
     * 				
     */
    public int exportData(String strName) {
    	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
    		LOG[lineCount++] = "@exportData: beginning";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
    		
    	}
    	
    	//this condition prevents wrong file from being accidentally deleted. File is cleared whenever printstream accesses it (how?), so writing the correct information 
    	//	into the desired file is paramount to data retention.
    	if(  ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) 
    	  || ( (strName == strWL)         && (fileSettings_WL > 0)         && (flag_WLImported == true) )
    	  || ( (strName == strWeights)    && (fileSettings_weights > 0)    && (flag_weightsImported == true) )
    	  || ( (strName == strQVals)      && (fileSettings_QVals > 0)	   && (flag_QValsImported == true) )	
    	  || ( (strName == strBPErrors)	  && (fileSettings_BPErrors > 0)   && (flag_BPErrorsImported == true) )	
    	  || ( (strName == strLog) ) 
    					){ 
	    	
    		PrintStream w = null;
	        
	        try {
	            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
	            // different commands between files
	            if (w.checkError()) {
	                //Error 0x03: cannot write
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Something done messed up (Error 6 cannot write)";
	            	}
	            	return ERROR_6_export_cannotWrite;
	            }
	            
	            //if scope for exporting files to stringTest
	            if ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) {
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into strStringTest:";
	            	}
	            	
	            	w.println(fileSettings_stringTest);
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into strStringTest.";
	            	}
	            	
	            	flag_stringTestImported = false;
	            } //end of testString

	            // weights
	            else if ( (strName == strWeights) && (fileSettings_weights > 0) && (flag_weightsImported == true) ) {
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into weights.dat:";
	            	}
	            	for (int i = 0; i < numInputsTotal; i++) {
		         		for (int j = 0; j < numHiddensTotal; j++) {
		         			w.println(arr_wIH[i][j]);
		                }
		         	} 
	            	for (int i = 0; i < numHiddensTotal; i++) {
		         		for (int j = 0; j < numOutputsTotal; j++) {
		         			w.println(arr_wHO[i][j]);
		                }
		         		w.println("999");
		         	}
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into weights.";
	            	}
	            	
	            	flag_weightsImported = false;
	            } //end weights export
	            
//	            winlose
	            else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into winLose:";
	            	}
	            	
	            	w.println(fileSettings_WL);
	            	w.println(totalFights+1);
	            	for (int i = 0; i < totalFights; i++){
	        			w.println(battleResults[i]);
	            	}
	        		w.println(currentBattleResult);
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into winLose.";
	            	}
	            	
	            	flag_WLImported = false;
	            }// end winLose
	            
	            //ARR_QVals
	            else if ( (strName == strQVals) && (fileSettings_QVals > 0) && (flag_QValsImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into QVals:";
	            	}
	            	
	            	w.println(fileSettings_QVals);
	            	w.println(totalQValRecords);
	            	for (int i = 0; i < totalQValRecords; i++){
	        			w.println(arr_QVals[i]);
	            	}
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into QVals.";
	            	}
	            	
	            	flag_QValsImported = false;
	            }// end QVals
	            
	            //ARR_BPErrors
	            else if ( (strName == strBPErrors) && (fileSettings_BPErrors > 0) && (flag_BPErrorsImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into BPErrors:";
	            	}
	            	
	            	w.println(fileSettings_BPErrors);
	            	w.println(totalBPErrorsRecords);
	            	for (int i = 0; i < totalBPErrorsRecords; i++){
	            		w.println(arr_BPErrors[i]);
	            	}
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into BPErrors.";
	            	}
	            }// end BPErrors
	            
	            //STRLOG
	            else if (strName == strLog) {
	            	//zeroes the log file in case it was filled from previous log session.
	            	if ((fileSettings_log & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE){
	            		w.print(0);
	            	}
	            	else{
		            	for (int i = 0; i < lineCount; i++){
		        			w.println(LOG[i]);
		            	}
	            	}
	            }
	            //end strLog
	            
	            
	            /* 
	             * add new files here - remember to add config settings and add to the beginning ifs
	             */
	                        
	            
	            else {
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "error 9";
	            		
	            	}
	            	return ERROR_9_export_dump;
	            }
	        }
	        
	        //OC: PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
	        catch (IOException e) {
	    		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out);
	            return ERROR_7_export_IOException;
	        } 
	        finally {
	            if (w != null) {
	                w.close();
	            }
	        }
	        
	        if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	        	LOG[lineCount++] = "(succeeded export)";
	        }
	        return SUCCESS_exportData;
    	} //end of big if.
    	
    	//this should prevent wipes by writing when data isn't ready or available. If import was successful, then fileSettings would already be set.
    	//goal is to prevent accidentally wiping irrelevant file.
    	else {
    		return ERROR_10_export_mismatchedStringName;
    	}
    }
 
    
    /**binaryActivation function
     * @param X
     * @return newVal. 
     */
 	public double binaryActivation(double X) {
 		double newVal = 1/(1 + Math.exp(-X)); 
 		return newVal;
 	}
 	
 	/**Function name: bipolarActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(X) = (2/(1 + e(-X))) - 1 
 	**/ 	
 	public double bipolarActivation(double X) {
 		double newVal = (2/(1 + Math.exp(-X)))-1; 
 		return newVal; 
 	}
 	
 	/** Function name: binaryDerivative
 	 * @param: input to take the derivative of based on f'(X) = f(X)*(1-f(X)). 
 	 * @return: derivative of value. 
 	 * 
 	 **/
 	public double binaryDerivative(double X) {
 		double binFunc = binaryActivation(X);
 		double binDeriv = binFunc*(1 - binFunc); 
 		return binDeriv;
 	}
 	
 	/** Function name: bipolarDerivative
 	 * @param: input to take the derivative of. 
 	 * @return: derivative of value: f'(X) =  0.5*(1 + f(X))*(1 - f(X));
 	 * 
 	 **/
 	public double bipolarDerivative(double X) {
 		double bipFunc = bipolarActivation(X);
 		double bipDeriv = 0.5*(1 + bipFunc)*(1 - bipFunc);  
 		return bipDeriv;
 	}
}


//TODO END OF ACTIVE CODE
/*
 *  Abandon all hope, ye who read below. Herein lies the graves of code obselete.
 */

	/* 
	 * If want to emphasize a certain event, then call the event and add external reward. 
	 * */
//	/**
//	* @name: 		onBulletMissed
//	* @purpose: 	1. Updates reward. -10 if bullet misses enemy
//	* @param:		1. HItBulletEvent class from Robot
//	* @return:		n
//	*/      
//    public void onBulletMissed(BulletMissedEvent event){
////    	reward += -5;    
////    	learningLoop(); 
////    	LOG[lineCount++] = "Missed Bullet" + reward;
//    }
    
//	/**
//	* @name: 		onBulletHit
//	* @purpose: 	1. Updates reward. +30 if bullet hits enemy
//	* 				2. Update the values of heading and energy of my robot 
//	* @param:		1. HItBulletEvent class from Robot
//	* @return:		n
//	*/     
//    public void onBulletHit(BulletHitEvent e){
//    	reward += 5; 
////    	LOG[lineCount++] = "Hit Bullet" + reward;
//    }
//    
//    /**
//     * @name: 		onHitWall
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
//    public void onHitWall(HitWallEvent e) {
//    	reward = -5; 
////    	LOG[lineCount++] = "Hit Wall" + reward;
//    }
    
//    /**
//     * @name: 		onHitByBullet
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
////    public void onHitByBullet(HitByBulletEvent e) {
////    	reward += -5;
////    //	learningLoop();
////    }   
    
//    /**
//     * @name: 		onHitRobot
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
////    public void onHitRobot(HitRobotEvent e) {
////    	reward = -1;
////    //	learningLoop();
////    }  
//	
	
 	
