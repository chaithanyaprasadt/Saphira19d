package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

/**
 * 压缩文件工具
 *
 * @author bwcx_jzy
 */
public class CompressionFileUtil {

	/**
	 * 解压文件
	 *
	 * @param compressFile 压缩文件
	 * @param destDir      解压到的文件夹
	 */
	public static void unCompress(File compressFile, File destDir) {
		Charset charset = CharsetUtil.CHARSET_GBK;
		charset = ObjectUtil.defaultIfNull(charset, CharsetUtil.defaultCharset());
		try {
			try (Extractor extractor = CompressUtil.createExtractor(charset, compressFile)) {
				extractor.extract(destDir);
			}
		} catch (Exception e) {
			CompressorInputStream compressUtilIn = null;
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(compressFile);
				compressUtilIn = CompressUtil.getIn(null, fileInputStream);
				if (compressUtilIn instanceof BZip2CompressorInputStream) {
					File file = FileUtil.file(destDir, BZip2Utils.getUncompressedFilename(compressFile.getName()));
					IoUtil.copy(compressUtilIn, new FileOutputStream(file));
				} else if (compressUtilIn instanceof GzipCompressorInputStream) {
					File file = FileUtil.file(destDir, GzipUtils.getUncompressedFilename(compressFile.getName()));
					IoUtil.copy(compressUtilIn, new FileOutputStream(file));
				} else {
					try (Extractor extractor = CompressUtil.createExtractor(charset, compressUtilIn)) {
						extractor.extract(destDir);
					}
				}
			} catch (Exception e2) {
				//
				e2.addSuppressed(e);
				//
				throw new RuntimeException(e2);
			} finally {
				IoUtil.close(fileInputStream);
				IoUtil.close(compressUtilIn);
			}
		}
	}


}
