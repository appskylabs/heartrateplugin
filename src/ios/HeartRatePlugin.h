#import <Cordova/CDVPlugin.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import "PulseDetector.h"
#import "Filter.h"
#import "AppDelegate.h"
#import <QuartzCore/QuartzCore.h>

typedef NS_ENUM(NSUInteger, CURRENT_STATE) {
    STATE_PAUSED,
    STATE_SAMPLING
};

#define MIN_FRAMES_FOR_FILTER_TO_SETTLE 10

@interface HeartRatePlugin : CDVPlugin <AVCaptureVideoDataOutputSampleBufferDelegate>{
}

// The hooks for our plugin commands
- (void)pluginInitialize :(CDVInvokedUrlCommand *)command;

@property (strong, nonatomic) CDVInvokedUrlCommand *mainCommand;
@property(nonatomic, strong) AVCaptureSession *session;
@property(nonatomic, strong) AVCaptureDevice *camera;
@property(nonatomic, strong) PulseDetector *pulseDetector;
@property(nonatomic, strong) Filter *filter;
@property(nonatomic, assign) CURRENT_STATE currentState;
@property(nonatomic, assign) int validFrameCounter;
@property (nonatomic) float pulse;

@end
