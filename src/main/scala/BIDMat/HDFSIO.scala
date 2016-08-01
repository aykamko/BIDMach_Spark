package BIDMat;

import java.io.File
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.SequenceFile.Metadata;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;

class SerText extends Text with Serializable {}

class HDFSIO extends HDFSIOtrait {

	def getCompressor(compress:Int):CompressionCodec = {
		import org.apache.hadoop.io.compress._;
			compress match {
			case 0 => new DefaultCodec();
			case 1 => new GzipCodec();
			case 2 => new Lz4Codec();
			case 3 => new SnappyCodec();
			case 4 => new BZip2Codec();
			}
	}

	def readMat(fname:String, omat:Mat):Mat = {
		val value = new MatIO;
		value.mat = omat;
		readThing(fname, value);
		value.mat.asMat;
	}
  
  def readMats(fname:String, omats:Array[ND]):Array[ND] = {
    val value = new MatIO;
    value.mats = omats;
    readThing(fname, value);
    value.mats;
  }
	
	def readND(fname:String, ond:ND):ND = {
		val value = new NDIO;
		value.nd = ond;
		readThing(fname, value);
		value.nd;
	}
  
  def readNDs(fname:String, ond:Array[ND]):Array[ND] = {
    val value = new NDIO;
    value.nds = ond;
    readThing(fname, value);
    value.nds;
  }
	
	def readThing(fname:String, value:Writable) = {
		val conf = new Configuration();
		val path = new Path(fname);
		val reader = new Reader(conf, Reader.file(path));
		val key = new SerText;
		reader.next(key, value);
		IOUtils.closeStream(reader);
	}

	def writeMat(fname:String, mat:Mat, compress:Int) = {
		val value = new MatIO;
		value.mat = mat;
		writeThing(fname, value, compress);
	}
  
  def writeMats(fname:String, mats:Array[ND], compress:Int) = {
    val value = new MatIO;
    value.mats = mats;
    writeThing(fname, value, compress);
  }

  def writeND(fname:String, mat:ND, compress:Int) = {
		val value = new NDIO;
		value.nd = mat;
		writeThing(fname, value, compress);
  }
  
  def writeNDs(fname:String, mats:Array[ND], compress:Int) = {
    val value = new NDIO;
    value.nds = mats;
    writeThing(fname, value, compress);
  }

	def writeThing(fname:String, value:Writable, compress:Int) = {
		val conf = new Configuration();
		val path = new Path(fname);
		val codec = getCompressor(compress);
		val key = new SerText;
		key.set(fname);
		val writer = SequenceFile.createWriter(conf, 
				Writer.file(path),
				Writer.keyClass(key.getClass()),
				Writer.valueClass(value.getClass()),
				Writer.compression(SequenceFile.CompressionType.BLOCK, codec));

		writer.append(key, value);
		IOUtils.closeStream(writer);
	}
  
  // Append a list of sequence files into a single file
  
  def appendFiles(ifnames:List[String], oname:String, compress:Int) = {
		val conf = new Configuration();
		val opath = new Path(oname);
		val codec = getCompressor(compress);
		val key = new SerText;
		var writer:Writer = null;
    var value:Writable = null;
    for (ifname <- ifnames) {
    	val ipath = new Path(ifname);
    	val reader = new Reader(conf, Reader.file(ipath));
      if (writer == null) {
    	  value = ReflectionUtils.newInstance(reader.getValueClass(), conf).asInstanceOf[Writable];
        writer = SequenceFile.createWriter(conf, 
        		Writer.file(opath),
        		Writer.keyClass(key.getClass()),
        		Writer.valueClass(value.getClass()),
        		Writer.compression(SequenceFile.CompressionType.BLOCK, codec));
      }
      while (reader.next(key, value)) {
    	  writer.append(key, value);   
      }
      IOUtils.closeStream(reader); 
    }
    IOUtils.closeStream(writer); 
  }
}