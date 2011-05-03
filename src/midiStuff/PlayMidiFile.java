package midiStuff;

import javax.sound.midi.*;
import javax.sound.sampled.DataLine.Info;
import java.io.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class PlayMidiFile extends Applet implements ActionListener{

	Button btn1= new Button("Browse");
	Button btn2= new Button("Play");
	Button btn3= new Button("Stop");
	
	File midiFile=new File("C:\\Documents and Settings\\User\\Desktop\\Heavy Tune.mid");
	
	Sequencer sequencer=null;
	
	public void init()
	{
		add(btn1);
		add(btn2);
		add(btn3);
		btn1.addActionListener(this);
		btn2.addActionListener(this);
		btn3.addActionListener(this);
		

	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource()==btn1)
		{
			Frame f= new Frame();
			
			FileDialog fileDialog=new FileDialog(f);
			
			fileDialog.setVisible(true);
			midiFile=new File(fileDialog.getDirectory()+fileDialog.getFile());
			if (!(midiFile.getName().toLowerCase().endsWith(".mid")||midiFile.getName().toLowerCase().endsWith(".midi")))
			{
				System.out.println("We both know that's not a MIDI file");
				System.exit(0);
			}
		}
		else if (e.getSource()==btn2)
		{
			try {
				sequencer = MidiSystem.getSequencer();
			} catch (MidiUnavailableException e1) {
				e1.printStackTrace();
			}
			try {
			    // Construct a Sequence object, and
			    // load it into my sequencer.
			    Sequence mySeq = MidiSystem.getSequence(midiFile);
			    sequencer.setSequence(mySeq);
			} catch (Exception e1) {
			   // Handle error and/or return
			}
			try {
				sequencer.open();
			} catch (MidiUnavailableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			sequencer.start();
		}
		else if (e.getSource()==btn3)
		{
			sequencer.stop();
		}
	}
}
