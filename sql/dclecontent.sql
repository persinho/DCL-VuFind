-- phpMyAdmin SQL Dump
-- version 3.4.5
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jul 18, 2012 at 07:14 PM
-- Server version: 5.5.16
-- PHP Version: 5.3.8

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `dclecontent`
--

-- --------------------------------------------------------

--
-- Table structure for table `acs_log`
--

CREATE TABLE IF NOT EXISTS `acs_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `acsTransactionId` varchar(50) DEFAULT NULL,
  `userAcsId` varchar(50) DEFAULT NULL,
  `fulfilled` tinyint(4) NOT NULL,
  `returned` tinyint(4) NOT NULL,
  `transactionDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='A trasaction log for transactions sent by the ACS server.';

-- --------------------------------------------------------

--
-- Table structure for table `db_update`
--

CREATE TABLE IF NOT EXISTS `db_update` (
  `update_key` varchar(100) NOT NULL,
  `date_run` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`update_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `econtent_attach`
--

CREATE TABLE IF NOT EXISTS `econtent_attach` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sourcePath` varchar(255) DEFAULT NULL,
  `dateStarted` int(11) NOT NULL,
  `dateFinished` int(11) DEFAULT NULL,
  `status` enum('running','finished') NOT NULL,
  `recordsProcessed` int(11) NOT NULL DEFAULT '0',
  `numErrors` int(11) DEFAULT '0',
  `notes` text,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COMMENT='A trasaction log for eContent that has been added to records.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_checkout`
--

CREATE TABLE IF NOT EXISTS `econtent_checkout` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The id of the eContent checkout',
  `recordId` int(11) NOT NULL COMMENT 'The id of the record being checked out',
  `dateCheckedOut` int(11) NOT NULL COMMENT 'When the item was checked out',
  `dateDue` int(11) NOT NULL COMMENT 'When the item needs to be returned',
  `dateReturned` int(11) DEFAULT NULL COMMENT 'When the item was returned',
  `userId` int(11) NOT NULL COMMENT 'The user who the hold is for',
  `status` enum('out','returned') DEFAULT NULL,
  `renewalCount` int(11) DEFAULT NULL COMMENT 'The number of times the item has been renewed.',
  `acsDownloadLink` varchar(512) DEFAULT NULL COMMENT 'The link to use when downloading an acs protected item',
  `dateFulfilled` int(11) DEFAULT NULL COMMENT 'When the item was fulfilled in the ACS server.',
  `downloadedToReader` tinyint(4) NOT NULL DEFAULT '0',
  `acsTransactionId` varchar(50) DEFAULT NULL,
  `userAcsId` varchar(50) DEFAULT NULL,
  `returnReminderNoticeSent` tinyint(4) NOT NULL DEFAULT '0',
  `recordExpirationNoticeSent` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`),
  KEY `UserStatus` (`userId`,`status`),
  KEY `status` (`status`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='EContent files that can be viewed within VuFind.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_file_packaging_log`
--

CREATE TABLE IF NOT EXISTS `econtent_file_packaging_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) DEFAULT NULL,
  `libraryFilename` varchar(255) DEFAULT NULL,
  `publisher` varchar(255) DEFAULT NULL,
  `distributorId` varchar(128) DEFAULT NULL,
  `copies` int(11) DEFAULT NULL,
  `dateFound` int(11) DEFAULT NULL,
  `econtentRecordId` int(11) DEFAULT NULL,
  `econtentItemId` int(11) DEFAULT NULL,
  `dateSentToPackaging` int(11) DEFAULT NULL,
  `packagingId` int(11) DEFAULT NULL,
  `acsError` mediumtext,
  `acsId` varchar(128) DEFAULT NULL,
  `status` enum('detected','recordFound','copiedToLibrary','itemGenerated','sentToAcs','acsIdGenerated','acsError','processingComplete','skipped') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `distributorId` (`distributorId`),
  KEY `publisher` (`publisher`),
  KEY `econtentItemId` (`econtentItemId`),
  KEY `status` (`status`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='A table to store information about diles that are being sent for packaging in the ACS server.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_history`
--

CREATE TABLE IF NOT EXISTS `econtent_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL COMMENT 'The id of the user who checked out the item',
  `recordId` int(11) NOT NULL COMMENT 'The record id of the item that was checked out',
  `openDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'The date the record was opened',
  `action` varchar(30) NOT NULL DEFAULT 'Read Online',
  `accessType` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='The econtent reading history for patrons';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_hold`
--

CREATE TABLE IF NOT EXISTS `econtent_hold` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The id of the eContent hold',
  `recordId` int(11) NOT NULL COMMENT 'The id of the record being placed on hold',
  `datePlaced` longtext NOT NULL COMMENT 'When the hold was placed',
  `dateUpdated` longtext COMMENT 'When the hold last changed status',
  `userId` int(11) NOT NULL COMMENT 'The user who the hold is for',
  `status` enum('active','suspended','cancelled','filled','available','abandoned') DEFAULT NULL,
  `reactivateDate` int(11) DEFAULT NULL COMMENT 'When the item should be reactivated.',
  `holdAvailableNoticeSent` tinyint(4) NOT NULL DEFAULT '0',
  `holdReminderNoticeSent` tinyint(4) NOT NULL DEFAULT '0',
  `holdAbandonedNoticeSent` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`),
  KEY `UserStatus` (`userId`,`status`),
  KEY `status` (`status`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='EContent files that can be viewed within VuFind.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_item`
--

CREATE TABLE IF NOT EXISTS `econtent_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The id of the eContent item',
  `filename` varchar(255) NOT NULL DEFAULT '' COMMENT 'The filename of the eContent item if any',
  `folder` varchar(100) NOT NULL DEFAULT '' COMMENT 'A folder containing a group of files for mp-3 files',
  `acsId` varchar(128) DEFAULT NULL COMMENT 'The uid of the book within the Adobe Content Server.',
  `recordId` int(11) NOT NULL COMMENT 'The id of the record to attach the item to.',
  `item_type` enum('epub','pdf','jpg','gif','mp3','plucker','kindle','externalLink','externalMP3','interactiveBook','overdrive') NOT NULL,
  `notes` varchar(255) NOT NULL DEFAULT '',
  `addedBy` int(11) NOT NULL DEFAULT '-1' COMMENT 'The id of the user who added the item or -1 if it was added automatically',
  `date_added` longtext NOT NULL COMMENT 'The date the item was added',
  `date_updated` longtext NOT NULL COMMENT 'The last time the item was changed',
  `reviewdBy` int(11) NOT NULL DEFAULT '-1' COMMENT 'The id of the user who added the item or -1 if not reviewed',
  `reviewStatus` enum('Not Reviewed','Approved','Rejected') NOT NULL DEFAULT 'Not Reviewed',
  `reviewDate` longtext COMMENT 'When the review took place.',
  `reviewNotes` mediumtext COMMENT 'Notes about the review',
  `link` varchar(500) DEFAULT NULL,
  `libraryId` int(11) NOT NULL DEFAULT '-1',
  `overDriveId` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='EContent files that can be viewed within VuFind.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_marc_import`
--

CREATE TABLE IF NOT EXISTS `econtent_marc_import` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) DEFAULT NULL,
  `dateStarted` int(11) NOT NULL,
  `dateFinished` int(11) DEFAULT NULL,
  `status` enum('running','finished') NOT NULL,
  `recordsProcessed` int(11) NOT NULL DEFAULT '0',
  `recordsWithErrors` int(11) NOT NULL DEFAULT '0',
  `errors` longtext,
  `supplementalFilename` varchar(255) DEFAULT NULL,
  `source` varchar(100) DEFAULT NULL,
  `accessType` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='A trasaction log for marc files imported into the database.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_rating`
--

CREATE TABLE IF NOT EXISTS `econtent_rating` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL COMMENT 'The id of the user who checked out the item',
  `recordId` int(11) NOT NULL COMMENT 'The record id of the item that was checked out',
  `dateRated` int(11) NOT NULL COMMENT 'The date the record was opened',
  `rating` int(11) NOT NULL COMMENT 'The rating to aply to the record',
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='The ratings for eContent records';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_record`
--

CREATE TABLE IF NOT EXISTS `econtent_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The id of the e-pub file',
  `cover` varchar(255) DEFAULT NULL COMMENT 'The filename of the cover art if any',
  `title` varchar(255) NOT NULL DEFAULT '',
  `subTitle` varchar(255) NOT NULL DEFAULT '',
  `accessType` enum('free','acs','singleUse') NOT NULL DEFAULT 'free' COMMENT 'Whether or not the Adobe Content Server should be checked before giving the user access to the file',
  `availableCopies` int(11) NOT NULL DEFAULT '1',
  `onOrderCopies` int(11) NOT NULL DEFAULT '0',
  `author` varchar(255) DEFAULT NULL,
  `author2` mediumtext,
  `description` mediumtext,
  `contents` mediumtext,
  `subject` mediumtext COMMENT 'A list of subjects separated by carriage returns',
  `language` varchar(255) NOT NULL DEFAULT '',
  `publisher` varchar(255) NOT NULL DEFAULT '',
  `edition` varchar(255) NOT NULL DEFAULT '',
  `isbn` varchar(500) DEFAULT NULL,
  `issn` varchar(255) DEFAULT NULL,
  `upc` varchar(255) DEFAULT NULL,
  `lccn` varchar(255) NOT NULL DEFAULT '',
  `series` varchar(255) NOT NULL DEFAULT '',
  `topic` mediumtext,
  `genre` mediumtext,
  `region` mediumtext,
  `era` varchar(255) NOT NULL DEFAULT '',
  `target_audience` varchar(255) NOT NULL DEFAULT '',
  `notes` mediumtext,
  `ilsId` varchar(255) DEFAULT NULL,
  `source` varchar(50) NOT NULL DEFAULT '' COMMENT 'Where the file was purchased or loaded from.',
  `sourceUrl` varchar(500) DEFAULT NULL COMMENT 'A link to the original file if known.',
  `purchaseUrl` varchar(500) DEFAULT NULL COMMENT 'A link to the url where a copy can be purchased if known.',
  `publishDate` varchar(100) DEFAULT NULL COMMENT 'The date the item was published',
  `addedBy` int(11) NOT NULL DEFAULT '-1' COMMENT 'The id of the user who added the item or -1 if it was added automatically',
  `date_added` int(11) NOT NULL COMMENT 'The date the item was added',
  `date_updated` int(11) DEFAULT NULL COMMENT 'The last time the item was changed',
  `reviewedBy` int(11) NOT NULL DEFAULT '-1' COMMENT 'The id of the user who added the item or -1 if not reviewed',
  `reviewStatus` enum('Not Reviewed','Approved','Rejected') NOT NULL DEFAULT 'Not Reviewed',
  `reviewDate` int(11) DEFAULT NULL COMMENT 'When the review took place.',
  `reviewNotes` mediumtext COMMENT 'Notes about the review',
  `trialTitle` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'Whether or not the title was purchased outright or on a trial basis.',
  `marcControlField` varchar(100) DEFAULT NULL COMMENT 'The control field from the marc record to avoid importing duplicates.',
  `collection` varchar(30) DEFAULT NULL,
  `marcRecord` mediumtext,
  `literary_form_full` varchar(30) DEFAULT NULL,
  `status` enum('active','deleted','archived') DEFAULT 'active',
  PRIMARY KEY (`id`),
  KEY `accessType` (`accessType`),
  KEY `source` (`source`),
  KEY `ECDateAdded` (`date_added`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='EContent records for titles that exist in VuFind, but not the ILS.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_record_detection_settings`
--

CREATE TABLE IF NOT EXISTS `econtent_record_detection_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fieldSpec` varchar(100) DEFAULT NULL,
  `valueToMatch` varchar(100) DEFAULT NULL,
  `source` varchar(100) DEFAULT NULL,
  `accessType` varchar(30) DEFAULT NULL,
  `item_type` varchar(30) DEFAULT NULL,
  `add856FieldsAsExternalLinks` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `source` (`source`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='A cache to store information about a user''s account within OverDrive.';

-- --------------------------------------------------------

--
-- Table structure for table `econtent_wishlist`
--

CREATE TABLE IF NOT EXISTS `econtent_wishlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL COMMENT 'The id of the user who checked out the item',
  `recordId` int(11) NOT NULL COMMENT 'The record id of the item that was checked out',
  `dateAdded` int(11) NOT NULL COMMENT 'The date the record was added to the wishlist',
  `status` enum('active','deleted','filled') NOT NULL COMMENT 'The status of the item in the wishlist',
  PRIMARY KEY (`id`),
  KEY `RecordId` (`recordId`),
  KEY `UserStatus` (`userId`,`status`),
  KEY `status` (`status`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='The ratings for eContent records';

-- --------------------------------------------------------

--
-- Table structure for table `overdrive_item`
--

CREATE TABLE IF NOT EXISTS `overdrive_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'The id of the eContent item',
  `format` varchar(100) NOT NULL DEFAULT '' COMMENT 'A description of the format from overdrive',
  `formatId` int(11) DEFAULT NULL COMMENT 'The id of the format ',
  `size` varchar(25) NOT NULL COMMENT 'A description of the size of the file(s) to be downloaded',
  `available` tinyint(4) DEFAULT NULL COMMENT 'Whether or not the format is available for immediate usage.',
  `notes` varchar(255) NOT NULL DEFAULT '',
  `lastLoaded` int(11) NOT NULL,
  `overDriveId` varchar(36) NOT NULL,
  `availableCopies` int(11) DEFAULT '0',
  `totalCopies` int(11) DEFAULT '0',
  `numHolds` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `OverDriveId` (`overDriveId`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COMMENT='Cached information about overdrive items within VuFind';

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
