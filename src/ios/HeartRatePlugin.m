//
//  HeartRatePlugin.m
//  HeartRateApp
//
//  Created by Taylor Korensky on 6/17/17.
//  Copyright © 2017 CMG Research Ltd. All rights reserved.


#import "HeartRatePlugin.h"

#import <Cordova/CDVAvailability.h>

#define MAX_PERIOD 1.5
#define MIN_PERIOD 0.1
#define INVALID_ENTRY -100

#define GAIN    1.894427025e+01

@implementation HeartRatePlugin {
    
    BOOL    TimerBool;
    //NSTimer   *timer;
}


- (void)pluginInitialize :(CDVInvokedUrlCommand *)command {
    NSLog(@"command id: %@", command.callbackId);
    self.mainCommand = [[CDVInvokedUrlCommand alloc] init];
    self.mainCommand = command;
    
    [self reset];
    [self startCameraCapture];
}

// start capturing frames
-(void) startCameraCapture {
    
    NSLog(@"main command id: %@", self.mainCommand.callbackId);
    // Create the AVCapture Session
    self.session = [[AVCaptureSession alloc] init];
    
    // Get the default camera device
    self.camera = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    
    
    
    
    // Create a AVCaptureInput with the camera device
    NSError *error=nil;
    
    AVCaptureInput* cameraInput = [[AVCaptureDeviceInput alloc] initWithDevice:self.camera error:&error];
    if (cameraInput == nil) {
        NSLog(@"Error to create camera capture:%@",error);
    }
    
    
    // Set the output
    AVCaptureVideoDataOutput* videoOutput = [
                                             [AVCaptureVideoDataOutput alloc] init];
    
    // create a queue to run the capture on
    dispatch_queue_t captureQueue= dispatch_queue_create("captureQueue", NULL);
    
    // setup ourself up as the capture delegate
    [videoOutput setSampleBufferDelegate:self queue:captureQueue];
    
    // configure the pixel format
    videoOutput.videoSettings = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithUnsignedInt:kCVPixelFormatType_32BGRA], (id)kCVPixelBufferPixelFormatTypeKey, nil];
    
    // and the size of the frames we want - we'll use the smallest frame size available
    [self.session setSessionPreset:AVCaptureSessionPresetLow];
    
    
    
    // Add the input and output
    [self.session addInput:cameraInput];
    [self.session addOutput:videoOutput];
    
    
    // Start the session
    // [self.session performSelectorInBackground:@selector(startRunning) withObject:nil];
    [self.session startRunning];
    
    // switch on torch mode - can't detect the pulse without it
    if([self.camera isTorchModeSupported:AVCaptureTorchModeOn]) {
        [self.camera lockForConfiguration:nil];
        [self.camera setActiveVideoMinFrameDuration:CMTimeMake(1, 10)];
        [self.camera setTorchMode:AVCaptureTorchModeOn];
        // set the minimum acceptable frame rate to 10 fps
        [self.camera unlockForConfiguration];
    }
    
    // we're now sampling from the camera
    self.currentState=STATE_SAMPLING;
    
    // stop the app from sleeping
    [UIApplication sharedApplication].idleTimerDisabled = YES;
    
    // update our UI on a timer every 0.1 seconds
    self.timer = [NSTimer scheduledTimerWithTimeInterval:0.1 target:self selector:@selector(update) userInfo:nil repeats:YES];
    
    
}

-(void) stopCameraCapture {
    
    [self.session stopRunning];
    self.session=nil;
}

#pragma mark Pause and Resume of pulse detection
-(void) pause {
    
    if(self.currentState==STATE_PAUSED) return;
    
    // switch off the torch
    if([self.camera isTorchModeSupported:AVCaptureTorchModeOn]) {
        [self.camera lockForConfiguration:nil];
        self.camera.torchMode=AVCaptureTorchModeOff;
        [self.camera unlockForConfiguration];
    }
    self.currentState=STATE_PAUSED;
    // let the application go to sleep if the phone is idle
    [UIApplication sharedApplication].idleTimerDisabled = NO;
}

-(void) resume {
    
    if(self.currentState!=STATE_PAUSED) return;
    
    // switch on the torch
    if([self.camera isTorchModeSupported:AVCaptureTorchModeOn]) {
        [self.camera lockForConfiguration:nil];
        [self.camera unlockForConfiguration];
    }
    self.currentState=STATE_SAMPLING;
    // stop the app from sleeping
    [UIApplication sharedApplication].idleTimerDisabled = YES;
    
}

// r,g,b values are from 0 to 1 // h = [0,360], s = [0,1], v = [0,1]
//    if s == 0, then h = -1 (undefined)
void RGBtoHSV( float r, float g, float b, float *h, float *s, float *v ) {
    
    float min, max, delta;
    min = MIN( r, MIN(g, b ));
    max = MAX( r, MAX(g, b ));
    *v = max;
    delta = max - min;
    if( max != 0 )
        *s = delta / max;
    else {
        // r = g = b = 0
        *s = 0;
        *h = -1;
        return;
    }
    if( r == max )
        *h = ( g - b ) / delta;
    else if( g == max )
        *h=2+(b-r)/delta;
    else
        *h=4+(r-g)/delta;
    *h *= 60;
    if( *h < 0 )
        *h += 360;
    
}


// process the frame of video
- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    
    
    // if we're paused don't do anything
    if(self.currentState==STATE_PAUSED) {
        // reset our frame counter
        NSLog(@"paused");
        self.validFrameCounter=0;
        return;
    }
    
    // this is the image buffer
    CVImageBufferRef cvimgRef = CMSampleBufferGetImageBuffer(sampleBuffer);
    // Lock the image buffer
    CVPixelBufferLockBaseAddress(cvimgRef,0);
    // access the data
    size_t width=CVPixelBufferGetWidth(cvimgRef);
    size_t height=CVPixelBufferGetHeight(cvimgRef);
    // get the raw image bytes
    uint8_t *buf=(uint8_t *) CVPixelBufferGetBaseAddress(cvimgRef);
    size_t bprow=CVPixelBufferGetBytesPerRow(cvimgRef);
    // and pull out the average rgb value of the frame
    float r=0,g=0,b=0;
    
    for(int y=0; y<height; y++) {
        for(int x=0; x<width*4; x+=4) {
            b+=buf[x];
            g+=buf[x+1];
            r+=buf[x+2];
        }
        buf+=bprow;
    }
    r/=255*(float) (width*height);
    g/=255*(float) (width*height);
    b/=255*(float) (width*height);
    
    // convert from rgb to hsv colourspace
    float h,s,v;
    RGBtoHSV(r, g, b, &h, &s, &v);
    // do a sanity check to see if a finger is placed over the camera
    if(s>0.5 && v>0.5) {
        
        // increment the valid frame count
        self.validFrameCounter++;
        // filter the hue value - the filter is a simple band pass filter that removes any DC component and any high frequency noise
        float filtered=[self processValue:h];
        NSLog(@"capturing: %f", filtered);
        // have we collected enough frames for the filter to settle?
        if(self.validFrameCounter > MIN_FRAMES_FOR_FILTER_TO_SETTLE) {
            // add the new value to the pulse detector
            [self addNewValue:filtered atTime:CACurrentMediaTime()];
        }
        
        TimerBool=YES;
        
        
    } else {
        
        
        TimerBool=NO;
        
        self.validFrameCounter = 0;
        // clear the pulse detector - we only really need to do this once, just before we start adding valid samples
        [self reset];
    }
    
}

-(float) update {
    
    NSLog(@"HERE");
    // if we're paused then there's nothing to do
    if(self.currentState==STATE_PAUSED)
        return 0;
    
    // get the average period of the pulse rate from the pulse detector
    float avePeriod=[self getAverage];
    if(avePeriod==INVALID_PULSE_PERIOD) {
        // no value available
        return 0;
        
        
    } else {
        // got a value so show it
        
        self.pulse = 60.0/avePeriod;
        
        NSLog(@"Pulse: %f", self.pulse);
        
        [self.timer invalidate];
        [self stopCameraCapture];
        
        NSString *string = nil;
        
        string = [NSString stringWithFormat: @"%f", self.pulse];
        
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:string];
        [self.commandDelegate sendPluginResult:result callbackId:self.mainCommand.callbackId];
        
        //delegate goes here
        return self.pulse;
    }
    
    return 0;
    
    
}



#pragma mark Pulse Detectior ********************************************

@synthesize periodStart;


- (id) init
{
    self = [super init];
    if (self != nil) {
        // set everything to invalid
        [self reset];
    }
    return self;
}

-(void) reset {
    
    for(int i=0; i<MAX_PERIODS_TO_STORE; i++) {
        periods[i]=INVALID_ENTRY;
    }
    for(int i=0; i<AVERAGE_SIZE; i++) {
        upVals[i]=INVALID_ENTRY;
        downVals[i]=INVALID_ENTRY;
    }
    
    freq=0.5;
    periodIndex=0;
    downValIndex=0;
    upValIndex=0;
    
}

-(float) addNewValue:(float) newVal atTime:(double) time {
    // we keep track of the number of values above and below zero
    
    if(newVal>0) {
        upVals[upValIndex]=newVal;
        upValIndex++;
        if(upValIndex>=AVERAGE_SIZE) {
            upValIndex=0;
        }
    }
    
    
    if(newVal<0) {
        downVals[downValIndex]=-newVal;
        downValIndex++;
        if(downValIndex>=AVERAGE_SIZE) {
            downValIndex=0;
        }
    }
    // work out the average value above zero
    float count=0;
    float total=0;
    for(int i=0; i<AVERAGE_SIZE; i++) {
        if(upVals[i]!=INVALID_ENTRY) {
            count++;
            total+=upVals[i];
        }
    }
    
    float averageUp=total/count;
    // and the average value below zero
    count=0;
    total=0;
    for(int i=0; i<AVERAGE_SIZE; i++) {
        if(downVals[i]!=INVALID_ENTRY) {
            count++;
            total+=downVals[i];
        }
    }
    float averageDown=total/count;
    
    // is the new value a down value?
    if(newVal<-0.5*averageDown) {
        wasDown=true;
    }
    
    // is the new value an up value and were we previously in the down state?
    if(newVal>=0.5*averageUp && wasDown) {
        wasDown=false;
        // work out the difference between now and the last time this happenned
        if(time-periodStart<MAX_PERIOD && time-periodStart>MIN_PERIOD) {
            
            periods[periodIndex]=time-periodStart;
            periodTimes[periodIndex]=time;
            periodIndex++;
            if(periodIndex>=MAX_PERIODS_TO_STORE) {
                periodIndex=0;
            }
            
        }
        // track when the transition happened
        periodStart=time;
    }
    // return up or down
    if(newVal<-0.5*averageDown) {
        return -1;
    } else if(newVal>0.5*averageUp) {
        return 1;
    }
    return 0;
}

-(float) getAverage {
    
    double time=CACurrentMediaTime();
    double total=0;
    double count=0;
    for(int i=0; i<MAX_PERIODS_TO_STORE; i++) {
        // only use upto 10 seconds worth of data
        if(periods[i]!=INVALID_ENTRY  && time-periodTimes[i]<10) {
            count++;
            total+=periods[i];
        }
    }
    // do we have enough values?
    if(count>2) {
        return total/count;
    }
    return INVALID_PULSE_PERIOD;
}

#pragma mark FILTER ************************************************************

-(float) processValue:(float) value {
    
    
    xv[0] = xv[1]; xv[1] = xv[2]; xv[2] = xv[3]; xv[3] = xv[4]; xv[4] = xv[5]; xv[5] = xv[6]; xv[6] = xv[7]; xv[7] = xv[8]; xv[8] = xv[9]; xv[9] = xv[10];
    
    xv[10] = value / GAIN;
    
    yv[0] = yv[1]; yv[1] = yv[2]; yv[2] = yv[3]; yv[3] = yv[4]; yv[4] = yv[5]; yv[5] = yv[6]; yv[6] = yv[7]; yv[7] = yv[8]; yv[8] = yv[9]; yv[9] = yv[10];
    
    yv[10] =   (xv[10] - xv[0]) + 5 * (xv[2] - xv[8]) + 10 * (xv[6] - xv[4])
    
    + ( -0.0000000000 * yv[0]) + (  0.0357796363 * yv[1])
    + ( -0.1476158522 * yv[2]) + (  0.3992561394 * yv[3])
    + ( -1.1743136181 * yv[4]) + (  2.4692165842 * yv[5])
    + ( -3.3820859632 * yv[6]) + (  3.9628972812 * yv[7])
    + ( -4.3832594900 * yv[8]) + (  3.2101976096 * yv[9]);
    
    return yv[10];
}


@end


