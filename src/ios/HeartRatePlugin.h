//
//  HeartRatePlugin.h
//  HeartRateApp
//
//  Created by Taylor Korensky on 6/17/17.
//  Copyright © 2017 CMG Research Ltd. All rights reserved.
//
#import <Cordova/CDVPlugin.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import "AppDelegate.h"
#import <QuartzCore/QuartzCore.h>

#define MAX_PERIODS_TO_STORE 20
#define AVERAGE_SIZE 20
#define INVALID_PULSE_PERIOD -1

#define NZEROS 10
#define NPOLES 10


typedef NS_ENUM(NSUInteger, CURRENT_STATE) {
    STATE_PAUSED,
    STATE_SAMPLING
};

#define MIN_FRAMES_FOR_FILTER_TO_SETTLE 10

@interface HeartRatePlugin : CDVPlugin <AVCaptureVideoDataOutputSampleBufferDelegate>{
    
    float upVals[AVERAGE_SIZE];
    float downVals[AVERAGE_SIZE];
    int upValIndex;
    int downValIndex;
    
    float lastVal;
    float periodStart;
    double periods[MAX_PERIODS_TO_STORE];
    double periodTimes[MAX_PERIODS_TO_STORE];
    
    int periodIndex;
    bool started;
    float freq;
    float average;
    
    bool wasDown;

    float xv[NZEROS+1], yv[NPOLES+1];
}

@property (strong, nonatomic) CDVInvokedUrlCommand *mainCommand;
@property(nonatomic, strong) AVCaptureSession *session;
@property(nonatomic, strong) AVCaptureDevice *camera;
@property(nonatomic, assign) CURRENT_STATE currentState;
@property(nonatomic, assign) int validFrameCounter;
@property (nonatomic) float pulse;

// The hooks for our plugin commands
- (void)pluginInitialize :(CDVInvokedUrlCommand *)command;

@property (nonatomic, assign) float periodStart;

//Pulse Detector
-(float) addNewValue:(float) newVal atTime:(double) time;
-(float) getAverage;
-(void) reset;

//Filter
-(float) processValue:(float) value;

@end
