
ServerLog : NetAddr {

	var <sent,<received;
	var lastStatus;
	*start { |server|
		var addr,new;
		if(Server.default.addr.isKindOf(ServerLog),{ ^Server.default.addr });
		addr = (server ? Server.default).addr;
		new = ServerLog(addr.hostname,addr.port);
		Server.default.addr = new;
		^new
	}
	*stop { |server|
		var addr,new;
		if(Server.default.addr.isKindOf(ServerLog).not,{ ^Server.default.addr });
		addr = (server ? Server.default).addr;
		new = NetAddr(addr.hostname,addr.port);
		Server.default.addr = new;
		^new
	}
	*gui { |tail=1000|
		if(Server.default.addr.isKindOf(ServerLog),{ 
			Server.default.addr.gui(nil,nil,tail)
		},{
			"ServerLog has not been running".inform;
		});
	}
	*report { |tail=1000|
		if(Server.default.addr.isKindOf(ServerLog),{ 
			Server.default.addr.report(tail)
		},{
			"ServerLog has not been running".inform;
		});
	}
	report { arg tail;
		this.events(tail).do({ |ev|
			ev.report
		})
	}

	


	// private
	*new { arg hostname, port=0;
		^super.new(hostname,port).slinit
	}
	
	slinit {
		thisProcess.recvOSCfunc = { arg time,replyAddr,msg;
			var status;
			if(msg[0] == 'status.reply') {
				status = msg[0..5];
				if(status != lastStatus,{
					received = received.add( ServerLogReceivedEvent(time,status) );
					lastStatus = status;
				});
			} {
				received = received.add( ServerLogReceivedEvent(time,msg) )
			}
		};
	}
	sendMsg { arg ... args;
		if(args != ["/status"],{
			sent = sent.add( ServerLogSentEvent( nil, args,false) );
		});
		^super.sendMsg(*args);
	}
	sendBundle { arg time ... args;
		sent = sent.add( ServerLogSentEvent( time,args,true) );
		^super.sendBundle(*([time]++args))
	}
	guiClass { ^ServerLogGui }
	
	events { arg tail;
		// list in logical time order
		var events,since;
		events = (sent ++ received);
		/*(if(numMinutes.notNil,{
			since = Main.elapsedTime - (numMinutes * 60);
			events = events.select({ |a| a.eventTime >= since });
		});*/
		events = events.sort({ |a,b| a.eventTime <= b.eventTime });
		if(tail.notNil,{
			^events.copyRange(events.size-tail-1,events.size-1);
		},{
			^events
		})
	}
	*cmdString { |cmd|
		if(cmd.asInteger != 0,{
			cmd = cmd.asInteger;
		});
		^cmd.switch(
			11 , { "/n_free" },
			12, {"/n_run"},
			14, {"/n_map"},
			48, {"/n_mapn"},
			15, {"/n_set"},
			16, {"/n_setn"},
			17, {"/n_fill"},
			10, {"/n_trace"},
			46, {"/n_query"},
			18, {"/n_before"},
			19, {"/n_after"},
			21, {"/g_new"},
			22, {"/g_head"},
			23, {"/g_tail"},
			24, {"/g_freeAll"},
			50, {"/g_deepFree"},
			9, {"/s_new"},
			44, {"/s_get"},
			45, {"/s_getn"},
			cmd.asString
		)
	}

}

ServerLogSentEvent { 

	var <>delta,<>msg,<>isBundle,<>timeSent;

	*new { arg delta,msg,isBundle;
		^super.newCopyArgs(delta,msg,isBundle,Main.elapsedTime)
	}
	eventTime {
		^timeSent + (delta?0)
	}
	report {
		var msgFormat;
		// if(isBundle,{  TODO
			// i use the gui mostly

		(">>> % (% + %) % %".format(this.eventTime,timeSent,delta,ServerLog.cmdString(msg[0]),msg.copyToEnd(1))).postln
	}

}

ServerLogReceivedEvent {

	var <>time,<>msg,<>timeReceived;

	*new { arg time,msg,isBundle;
		^super.newCopyArgs(time,msg,Main.elapsedTime)
	}
	eventTime {
		^time
	}
	report {
		var cmd, one, numUGens, numSynths, numGroups, numSynthDefs, 
					avgCPU, peakCPU, sampleRate, actualSampleRate;
		if(msg[0] == 'status.reply',{
			#cmd, one, numUGens, numSynths, numGroups, numSynthDefs, 
					avgCPU, peakCPU, sampleRate, actualSampleRate = msg;
			("<<< % % ugens % synths % groups % synthDefs".format(this.eventTime,numUGens,numSynths,numGroups,numSynthDefs)).postln
		},{
			("<<< % % %".format(this.eventTime,ServerLog.cmdString(msg[0]),msg.copyToEnd(1))).postln;
		});
	}
	isBundle {
		^false
	}
}




		
	
