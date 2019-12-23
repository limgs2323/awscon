window.logr = (function(){
	let _levels = {ALL:10,DEBUG:10,INFO:20,WARN:30,ERROR:40,OFF:50},
		_storeLogger = {},
		_logGroups = {},
		_defaultLevel = _levels.OFF;
		_isPrintTime = false;
		_createLogger = function(logGrp){
			"use strict";
			if( !logGrp ){ logGrp = "" };
			let _logger = (function(){
				let _logGrp = logGrp;
				return {
					log:function(){
						"use strict";
						if( logr.isPrint(_logGrp, logr.DEBUG) ){
							let cArgu = Array.from(arguments);
							cArgu[0] = logr.getTime()+"#"+_logGrp+"# "+cArgu[0];
							console.log.apply(console, cArgu);
						}
					},
					info:function(){
						"use strict";
						if( logr.isPrint(_logGrp, logr.INFO) ){
							let cArgu = Array.from(arguments);
							cArgu[0] = logr.getTime()+"#"+_logGrp+"# "+cArgu[0];
							console.info.apply(console, cArgu);
						}
					},
					warn:function(){
						"use strict";
						if( logr.isPrint(_logGrp, logr.WARN) ){
							let cArgu = Array.from(arguments);
							cArgu[0] = logr.getTime()+"#"+_logGrp+"# "+cArgu[0];
							console.warn.apply(console, cArgu);
						}
					},
					error:function(){
						"use strict";
						if( logr.isPrint(_logGrp, logr.ERROR) ){
							let cArgu = Array.from(arguments);
							cArgu[0] = logr.getTime()+"#"+_logGrp+"# "+cArgu[0];
							console.error.apply(console, cArgu);
						}
					}
				}
			}());
			if( "" != logGrp) {
				_storeLogger[logGrp] = _logger;
			}
			return _logger;
		},
		_convertLogLevelString = function(level){
			"use strict";
			let msg;
			if( _defaultLevel <= logr.DEBUG) {
				msg = "DEBUG";
			} else if( _defaultLevel <= logr.INFO) {
				msg = "INFO";
			} else if( _defaultLevel <= logr.WARN) {
				msg = "WARN";
			} else if( _defaultLevel <= logr.ERROR) {
				msg = "ERROR";
			} else if( _defaultLevel <= logr.OFF) {
				msg = "OFF";
			} else {
				msg = "Unrecognizable Log Level";
			}
			return msg
		},
		_convertLogLevelNumber = function(level){
			"use strict";
			if( !level ){
				return _defaultLevel;
			} else {
				if( typeof level === "string" ){
					let arrKeys = Object.getOwnPropertyNames(_levels);
					if( arrKeys.includes(level.toUpperCase()) ){
						return _levels[level.toUpperCase()];
					}
				} else if( typeof level === "number" ){
					let arrKeys = Object.getOwnPropertyNames(_levels),
						isFind = arrKeys.some(function(key){
								return _levels[key] == level
							});
					if( isFind ){
						return level;
					}
				}
			}
			throw "Unrecognizable Log Level."; //return 안걸리면 throw
		}
		_defaultLogger = _createLogger();
	return {
		ALL:_levels.ALL,
		DEBUG:_levels.DEBUG,
		INFO:_levels.INFO,
		WARN:_levels.WARN,
		ERROR:_levels.ERROR,
		OFF:_levels.OFF,
		log:function(){
			"use strict";
			logr.getLogger().log.apply(logr.getLogger(), arguments);
		},
		info:function(){
			"use strict";
			logr.getLogger().info.apply(logr.getLogger(), arguments);
		},
		warn:function(){
			"use strict";
			logr.getLogger().warn.apply(logr.getLogger(), arguments);
		},
		error:function(){
			"use strict";
			logr.getLogger().error.apply(logr.getLogger(), arguments);
		},
		logGrp:function(){
			"use strict";
			let cArgu = Array.from(arguments);
			if( cArgu.length > 1){
				let logGroup = cArgu.splice(0,1)[0];
				logr.getLogger(logGroup).log.apply(logr.getLogger(logGroup), cArgu);
			}
		},
		infoGrp:function(){
			"use strict";
			let cArgu = Array.from(arguments);
			if( cArgu.length > 1){
				let logGroup = cArgu.splice(0,1)[0];
				logr.getLogger(logGroup).info.apply(logr.getLogger(logGroup), cArgu);
			}
		},
		warnGrp:function(){
			"use strict";
			let cArgu = Array.from(arguments);
			if( cArgu.length > 1){
				let logGroup = cArgu.splice(0,1)[0];
				logr.getLogger(logGroup).warn.apply(logr.getLogger(logGroup), cArgu);
			}
		},
		errorGrp:function(){
			"use strict";
			let cArgu = Array.from(arguments);
			if( cArgu.length > 1){
				let logGroup = cArgu.splice(0,1)[0];
				logr.getLogger(logGroup).error.apply(logr.getLogger(logGroup), cArgu);
			}
		},
		printAll:function(){
			"use strict";
			logr.printDefaultLogLevel();
			logr.printLogGroup();
			logr.printStoreLogger();
		},
		//getLogger 약식
		get:function(logGrp){
			"use strict";
			return logr.getLogger(logGrp);
		},
		getLogger:function(logGrp){
			"use strict";
			if( !logGrp ){
				return _defaultLogger;
			} else if( (typeof logGrp) !== "string" ){
				console.error("Log group accepts only strings.");
				return;
			}
			if( !_storeLogger[logGrp] ){
				return _createLogger(logGrp);
			} else {
				return _storeLogger[logGrp];
			}
		},
		printStoreLogger:function(){
			"use strict";
			console.table(_storeLogger);
		},
		clearStoreLogger:function(){
			"use strict";
			_storeLogger = {};
		},
		printLogGroup:function(){
			"use strict";
			console.info("Log Level","::",JSON.stringify(_levels));
			console.table(_logGroups);
		},
		clearLogGroup:function(){
			"use strict";
			_logGroups = {};
		},
		printDefaultLogLevel:function(){
			"use strict";
			console.info("Default Log Level","::",_convertLogLevelString(_defaultLevel));
		},
		setPrintTime:function(flag){
			_isPrintTime = flag;
		},
		getTime:function(){
			if( _isPrintTime ){
				return mlw.Util.getNow();
			} else {
				return "";
			}
		},
		getDefaultLogLevel:function(){
			"use strict";
			return _defaultLevel;
		},
		//디폴트 로거와 로그 그룹이 없는 로거에 적용될 로그레벨 세팅
		setDefaultLogLevel:function(level){
			"use strict";
			if( !level ){
				_defaultLevel = _levels.ALL;
			} else {
				try{
					_defaultLevel = _convertLogLevelNumber(level);
				} catch(e){
					console.error(e, "Default Log Level set OFF.");
					_defaultLevel = _levels.OFF;
				}
			}
			logr.printDefaultLogLevel();
		},
		setAllGroup:function(level){
			"use strict";
			try {
				let arrKeys = Object.getOwnPropertyNames(_logGroups);
				level = _convertLogLevelNumber(level);
				arrKeys.forEach(function(key){
					_logGroups[key] = level;
				}, logr);
			} catch (e) {
				console.error(e);
			}
			
		},
		isPrint:function(logGroup, level){
			"use strict";
			let findLogLevel;
			if( !logGroup || "" == logGroup ){
				findLogLevel = _defaultLevel;
			} else {
				if( (typeof logGroup) === "string" ){
					if( "true" === logGroup.toLowerCase() ){
						findLogLevel = _levels.ALL;
					} else {
						let key, isFind = false, arr = logGroup.split(".");
						for( let i = arr.length-1; i >= 0; i-- ){
							key = arr.join(".");
							if( (typeof _logGroups[key]) === "number" ){
								findLogLevel = _logGroups[key];
								isFind = true;
								break;
							} else {
								arr.pop();
								continue;
							}
						}
						if( !isFind ){
							findLogLevel = _defaultLevel;
						}
					}
				}
			}
			return (findLogLevel <= level);
		},
		//setLogGroup("mlu.ui"); //ALL 로 세팅
		//setLogGroup("mlu.ui", logr.ALL); //자기 그룹만 ALL 로 세팅
		//setLogGroup("mlu.ui", "ALL"); //자기 그룹만 ALL 로 세팅
		//setLogGroup("mlu.ui", logr.ALL, true); //부모까지 ALL로 세팅
		setLogGroup:function(logGroup, level, setParent){
			"use strict";
			try{
				if( !setParent || typeof setParent != "boolean" ){ setParent = false; }
				if( !logGroup || "" == logGroup ){ throw "Log Group is null"; }
				if( !level ){
					level = logr.ALL;
				} else {
					level = _convertLogLevelNumber(level);
				}

				let key, isNotValid = false, arr = logGroup.split(".");
				arr.forEach(function(value){ if( !value || "" == value ) isNotValid = true; })
				if( isNotValid ){ throw "["+logGroup+"] is not valid format"; }

				if( !_logGroups ){ _logGroups = {}; }
				for( let i = arr.length-1; i >= 0; i-- ){
					key = arr.join(".");
					if( logGroup == key ){
						_logGroups[key] = level;
					} else {
						if( setParent ){
							_logGroups[key] = level;
						} else {
							if( (typeof _logGroups[key]) !== "number" ){//이미 설정되어 있으면 넘어감
								_logGroups[key] = logr.OFF;
							}
						}
					}
					arr.pop();
					console.info("Debug Group %s::%s",key,_logGroups[key]);
				}
			} catch(e){
				console.error(e);
			}
		}
	}
}());
