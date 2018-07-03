
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;



public class HardwareControl {

    GpioPinDigitalInput ledVerteCarteVideo;
    GpioPinDigitalOutput onOffCarteVideo;
    GpioPinDigitalOutput backLight;
    GpioPinDigitalInput spiData;
    GpioPinDigitalOutput spiCS;
    GpioPinDigitalOutput spiClk;

	public void setBackLight(boolean v) {
		backLight.setState(v);
	}
	
	public void setAlimCarteVideo(boolean v) throws Exception {

		if(v==true) {
			if(ledVerteCarteVideo.isHigh()==true) return;
		}
		if(v==false) {
			if(ledVerteCarteVideo.isHigh()==false) return;
		}

		onOffCarteVideo.setState(false);
		Thread.sleep(1000);
		onOffCarteVideo.setState(false);
	}
	
	public boolean getLedState() {
		return ledVerteCarteVideo.isHigh();
	}

	// waitTime in µs
	void waitTime(long waitTime) {
		long startTime=System.nanoTime();
		while(((System.nanoTime()-startTime)/1000)<waitTime) Thread.yield();
	}
	
	public int readVoltage() {
		int 	i, val=0, mask;
		boolean bit;
		
		spiCS.setState(PinState.LOW);
		waitTime(100);
		
		for(i=1; i<=16; i++) {
			spiClk.setState(PinState.LOW);
			waitTime(100);
			
			mask=0;
			switch(i) {
			case 4 : mask=0x80; break;
			case 5 : mask=0x40; break;
			case 6 : mask=0x20; break;
			case 7 : mask=0x10; break;
			case 8 : mask=0x08; break;
			case 9 : mask=0x04; break;
			case 10 : mask=0x02; break;
			case 11 : mask=0x01; break;
			}
			
			bit=spiData.isHigh();
			if(bit) val|=mask;
			
			spiClk.setState(PinState.HIGH);
			waitTime(100);
		}
		
		spiCS.setState(PinState.HIGH);
		
		return val;
	}
	
	
    public HardwareControl() throws Exception {
        
        System.out.println("<--Pi4J--> GPIO Multipurpose Pin Example ... started.");
    
        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();


        ledVerteCarteVideo = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, "ledVerteCarteVideo", PinPullResistance.PULL_DOWN);
        onOffCarteVideo = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "OnOffCarteVideo", PinState.HIGH);
        backLight = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "backLight", PinState.LOW);
        spiData = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, "spiData", PinPullResistance.OFF);
        spiCS = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "spiCS", PinState.HIGH);
        spiClk = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "spiClk", PinState.HIGH);

        spiCS.setState(PinState.HIGH);        
    }

}
