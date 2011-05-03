package midiStuff;
import java.applet.*;
import java.awt.*;

import javax.sound.midi.*;
import javax.sound.sampled.DataLine.Info;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
public class MidiInPitchBends extends Applet implements MouseListener, MouseMotionListener, ChangeListener, Runnable, ComponentListener, ItemListener{

	Label lblMax=new Label("Bend Range (semitones):");
	JSpinner txtMax=new JSpinner(new SpinnerNumberModel(2,0,24,1));
	Label lblRelease=new Label("Release: ");
	Checkbox chkRelease=new Checkbox();
	
	int max=3;
	
	double currentPitch=0;
	double goalPitch=0;
	
	int sx=400;
	int sy=400;
	
	Thread t=new Thread(this);
	
	boolean bending=false;	//determines whether we are bending to a certain pitch
	boolean releasing=false;	//determines if we are releasing a pitch
	/*note that releasing needs to exist: if the current pitch is 0, we'd get a bunch of
	 pitch bend events that aren't necessary
	 */
	//the device that we will be sending midi events to.
	MidiDevice midiOut=null;
	
	public void init()
	{
		Panel labels=new Panel();
		labels.setLayout(new GridLayout(0,1));
		labels.add(lblMax);
		labels.add(lblRelease);
		
		
		
		Panel textFields=new Panel();
		textFields.setLayout(new GridLayout(0,1));
		textFields.add(txtMax);
		textFields.add(chkRelease);
		
		Panel settings=new Panel();
		settings.setLayout(new BorderLayout());
		settings.add(labels);
		settings.add(textFields,BorderLayout.EAST);
		
		Panel flowLayout=new Panel();
		flowLayout.setLayout(new FlowLayout());
		flowLayout.add(settings);
		
		setLayout(new BorderLayout());
		add(flowLayout,BorderLayout.WEST);

		txtMax.addChangeListener(this);
		
		setSize(sx,sy);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		chkRelease.addItemListener(this);
		
		t.start();
		
		try {
			for (int i=0;i<MidiSystem.getMidiDeviceInfo().length;i++)
			{
				System.out.println(i+": "+MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[i]));
				System.out.println(MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[i]).getDeviceInfo().getName());
			}
			System.out.println();
			System.out.println(MidiSystem.getReceiver()+" is the current receiver");
			System.out.println(MidiSystem.getTransmitter()+" is the current transmitter");
			//makes the program do what my keyboard tells it to do
			MidiDevice midiIn=MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[0]);
			//usually [3] for SW synth on my system
			//usually [1] to bend the piano itself
			midiOut =MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo()[3]);
			midiIn.open();
			midiOut.open();
			midiIn.getTransmitter().setReceiver(midiOut.getReceiver());
			
			System.out.println(MidiSystem.getReceiver()+" is the current receiver");
			System.out.println(MidiSystem.getTransmitter()+" is the current transmitter");
			
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void mouseDragged(MouseEvent e) {
		//translate where the user's mouse is into the goal pitch of the note
		if (e.getY()<sy/2-50)
		{
			bending=true;
			releasing=false;
			
			goalPitch=1-e.getY()/(sy/2-50.0);
			if (goalPitch>1)
			{
				goalPitch=1;
			}
		}
		else if (e.getY()>sy/2+50)
		{
			bending=true;
			releasing=false;
			
			goalPitch=-1*(e.getY()-(sy/2+50))/(sy/2-50.0);
			if (goalPitch<-1)
			{
				goalPitch=-1;
			}
		}
		else
		{
			bending=false;
			releasing=true;
		}
		
		
		System.out.println("Goal pitch: "+goalPitch);
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public void stateChanged(ChangeEvent e) {
		if (e.getSource()==txtMax)
		{
			//txtMax was changed. This is the bending range in semitones
			try{
				Receiver rec=midiOut.getReceiver();
				max=(Integer)txtMax.getValue();
				//Send a a few MIDI messages to midiOut that sets the new bend range
				ShortMessage changeBendRange1=new ShortMessage();
				changeBendRange1.setMessage(ShortMessage.CONTROL_CHANGE,0,101,0);
				rec.send(changeBendRange1, -1);
				
				ShortMessage changeBendRange2=new ShortMessage();
				changeBendRange2.setMessage(ShortMessage.CONTROL_CHANGE,0,100,0);
				rec.send(changeBendRange2, -1);
				
				ShortMessage changeBendRange3=new ShortMessage();
				//last parameter is the range
				changeBendRange3.setMessage(ShortMessage.CONTROL_CHANGE,0,6,max);
				rec.send(changeBendRange3, -1);
				
				ShortMessage changeBendRange4=new ShortMessage();
				changeBendRange4.setMessage(ShortMessage.CONTROL_CHANGE,0,38,0);
				rec.send(changeBendRange4, -1);
			}
			catch (MidiUnavailableException mue)
			{
				
			}
			catch (InvalidMidiDataException imde)
			{
				
			}
		}
	}

	public void run() {
		while (true)
		{
			if (bending&releasing)
			{
				//Not supposed to happen. Just debuggin'
				System.out.println("bending and releasing?");
			}
			else if (bending)
			{
				ShortMessage message=new ShortMessage();
				if (currentPitch==goalPitch)
				{
					bending=false;
				}
				else
				{
					try {
						//send a pitch bend message to MIDI out
						Receiver rec=midiOut.getReceiver();
						ShortMessage pitchBend=new ShortMessage();
						pitchBend.setMessage(ShortMessage.PITCH_BEND,0,63+(int)(63*currentPitch),63+(int)(63*goalPitch));
						rec.send(pitchBend, -1);
						currentPitch=goalPitch;
					} catch (MidiUnavailableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (InvalidMidiDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else if (releasing)
			{
				System.out.println("releasing");
				try{
					//Send a MIDI message to MIDI out to release the note
					Receiver rec=midiOut.getReceiver();
					ShortMessage pitchBend=new ShortMessage();
					pitchBend.setMessage(ShortMessage.PITCH_BEND,0,63+(int)(63*currentPitch),63);
					rec.send(pitchBend,-1);
					currentPitch=0;
					releasing=false;
				}
				catch(MidiUnavailableException e)
				{
					
				}
				catch(InvalidMidiDataException e)
				{
					
				}
			}
			
			
			
			try {
				t.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		//similar to mouse dragged, but we set bending and releasing variables
		if (e.getY()<sy/2-50)
		{
			bending=true;
			releasing=false;
			
			goalPitch=1-e.getY()/(sy/2-50.0);
			if (goalPitch>1)
			{
				goalPitch=1;
			}
		}
		else if (e.getY()>sy/2+50)
		{
			bending=true;
			releasing=false;
			
			goalPitch=-1*(e.getY()-(sy/2+50))/(sy/2-50.0);
			if (goalPitch<-1)
			{
				goalPitch=-1;
			}
		}
		else
		{
			bending=false;
			releasing=true;
		}
		
		
		System.out.println("Goal pitch: "+goalPitch);
		
	}

	public void mouseReleased(MouseEvent arg0) {
		if (chkRelease.getState())
		{
			bending=false;
			releasing=true;
		}
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void componentResized(ComponentEvent e) 
	{
		sy=(int)getHeight();
		sx=(int)getWidth();
		System.out.println(sy);
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void paint(Graphics g)
	{
		//use Graphics2D 4 sum phancy graphics
		Graphics2D g2d=(Graphics2D) g;
		g2d.setColor(Color.GREEN);
		g2d.fillRect(0, sy/2-50, sx,100);
		
		GradientPaint gp=new GradientPaint(0,0,Color.WHITE,0,sy/2-50,Color.GREEN);
		g2d.setPaint(gp);
		g2d.fillRect(0,0,sx,sy/2-50);
		gp=new GradientPaint(0,sy/2+50,Color.GREEN,0,sy,Color.BLACK);
		g2d.setPaint(gp);
		g2d.fillRect(0,sy/2+50,sx,sy);
		g2d.setColor(Color.BLACK);
		g2d.drawString("Drag mouse here to bend", sx-150, 20);
		
	}


	
	public void itemStateChanged(ItemEvent e) 
	{
		if (e.getStateChange()==1)
		{
			releasing=true;
		}
		
	}
}
